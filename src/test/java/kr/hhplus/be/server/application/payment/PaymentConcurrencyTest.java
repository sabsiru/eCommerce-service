package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
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

        CreateOrderCommand command = new CreateOrderCommand(
                user.getId(),
                List.of(new OrderItemCommand(product.getId(), 1, product.getPrice()))
        );
        Order order = Order.create(command.getUserId(), command.getOrderItemCommands());
        order = orderService.save(order);

        int totalAmount = order.getTotalAmount();

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            Order finalOrder = order;
            executorService.submit(() -> {
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

        Order order = orderService.save(
                Order.create(user.getId(), List.of(new OrderItemCommand(product.getId(), 1, 10000)))
        );

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

        // then
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments.size()).isLessThanOrEqualTo(1);
    }

    @Test
    void 재고_1개_상품_동시_결제_요청_검증() throws InterruptedException {
        // given
        User user1 = userRepository.save(User.create("유저1", 50000));
        User user2 = userRepository.save(User.create("유저2", 50000));
        User user3 = userRepository.save(User.create("유저3", 50000));
        User user4 = userRepository.save(User.create("유저4", 50000));
        User user5 = userRepository.save(User.create("유저5", 50000));

        List<User> users = List.of(user1, user2, user3, user4, user5);

        Product product = productRepository.save(new Product("한정판", 10000, 1, 1L));

        OrderItemCommand orderItem = new OrderItemCommand(product.getId(), 1, product.getPrice());

        List<Long> paymentIds = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(users.size());
        ExecutorService executor = Executors.newFixedThreadPool(users.size());

        for (User user : users) {
            executor.submit(() -> {
                try {
                    Order order = orderService.create(new CreateOrderCommand(user.getId(), List.of(orderItem)));
                    orderService.save(order);

                    Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());
                    if (payment.getStatus() == PaymentStatus.COMPLETED) {
                        paymentIds.add(payment.getId());
                    }
                } catch (Exception e) {
                    // 무시 (실패 케이스 발생 허용)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        System.out.println("▶ 성공한 결제 건수: " + paymentIds.size());
        assertThat(paymentIds).hasSize(1); // 재고 1개이므로 1건만 성공해야 정상
    }

    @Test
    void 동일_결제건_중복_환불_시도_포인트_중복적립_및_히스토리_검증() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("유저", 10000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));
        Order order = orderService.create(new CreateOrderCommand(user.getId(),
                List.of(new OrderItemCommand(product.getId(), 1, 10000))));
        orderService.save(order);
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

        // then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        List<PointHistory> history = pointHistoryRepository.findByUserId(user.getId());

        System.out.println("▶ 최종 포인트: " + updatedUser.getPoint());
        System.out.println("▶ 히스토리 수: " + history.size());

        assertThat(updatedUser.getPoint()).isLessThan(10000); // 1건 이상 적립되었으면 실패
        assertThat(history).hasSizeLessThan(2); // 정상이라면 최대 1건만 기록
    }

    /*domian 에서 재고를 관리하고 있어서 테스트환경에선 방어가 되는걸까요?? 포인트나 재고관련한 테스트는 재고나 포인트가
    * 누적이 일어 나지 않는거 같습니다.*/
    @Test
    void 동일_결제건_중복_환불_시도_재고_중복복원_검증() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("유저", 10000));
        Product product = productRepository.save(new Product("상품", 10000, 1, 1L)); // 초기 재고 0

        Order order = orderService.create(new CreateOrderCommand(
                user.getId(),
                List.of(new OrderItemCommand(product.getId(), 1, 10000))
        ));
        orderService.save(order);

        // 결제 진행 → 재고 차감
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

        // then
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

        System.out.println("▶ 최종 재고: " + updatedProduct.getStock());

        assertThat(updatedProduct.getStock()).isLessThanOrEqualTo(1); // 재고가 2 이상이면 중복 복원 발생
    }

}