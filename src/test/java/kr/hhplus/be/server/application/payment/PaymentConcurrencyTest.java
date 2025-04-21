package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderLine;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.PointHistoryRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentConcurrencyTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OrderService orderService;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;


    @Test
    void 결제_동시성_테스트_중복결제_검증() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("동시결제자", 50000));
        Product product = productRepository.save(new Product("테스트상품", 10000, 5, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);
        int totalAmount = order.getTotalAmount();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            Order finalOrder = order;
            executor.submit(() -> {
                try {
                    paymentFacade.processPayment(finalOrder.getId(), totalAmount);
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("user.getPoint() = " + user.getPoint());
        // then
        List<Payment> all = paymentRepository.findAll();
        assertThat(all).hasSize(1); // 한 번만 결제 성공해야 함
    }

    @Test
    void 재고_부족_상황_동시_결제_테스트() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("동시성테스트유저", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 1, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentFacade.processPayment(order.getId(), 10000);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        // then
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments.size()).isLessThanOrEqualTo(1);
    }

    @Test
    void 재고_1개_상품_동시_결제_요청_검증() throws InterruptedException {
        // given
        List<User> users = List.of(
                userRepository.save(User.create("유저1", 50000)),
                userRepository.save(User.create("유저2", 50000)),
                userRepository.save(User.create("유저3", 50000)),
                userRepository.save(User.create("유저4", 50000)),
                userRepository.save(User.create("유저5", 50000))
        );

        Product product = productRepository.save(new Product("한정판", 10000, 1, 1L));

        List<Long> paymentIds = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(users.size());
        ExecutorService executor = Executors.newFixedThreadPool(users.size());

        for (User user : users) {
            executor.submit(() -> {
                try {
                    List<OrderLine> ln = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
                    Order order = orderService.create(user.getId(), ln);

                    Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());
                    if (payment.getStatus() == PaymentStatus.COMPLETED) {
                        paymentIds.add(payment.getId());
                    }
                } catch (Exception ignored) {
                    // 실패 케이스 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("▶ 성공한 결제 건수: " + paymentIds.size());
        assertThat(paymentIds).hasSize(1); // 재고가 1개이므로 1건만 결제 성공
    }

    @Test
    void 동일_결제건_중복_환불_시도_포인트_중복적립_및_히스토리_검증() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("유저", 10000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));
        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);
        Payment payment = paymentFacade.processPayment(order.getId(), 10000);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentFacade.processRefund(payment.getId());
                } catch (Exception ignored) {
                    // 중복 환불 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        // then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        List<PointHistory> history = pointHistoryRepository.findByUserId(user.getId());

        System.out.println("▶ 최종 포인트: " + updatedUser.getPoint());
        System.out.println("▶ 히스토리 수: " + history.size());

        assertThat(updatedUser.getPoint()).isLessThan(10000); // 1건 이상 적립되었으면 실패
        assertThat(history).hasSizeLessThan(2); // 정상이라면 최대 1건만 기록
    }

}