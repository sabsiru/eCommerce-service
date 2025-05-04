package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderLine;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentConcurrencyTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OrderService orderService;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;


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
        AtomicInteger exceptionCount = new AtomicInteger();
        for (User user : users) {
            executor.submit(() -> {
                try {
                    List<OrderLine> ln = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
                    Order order = orderService.create(user.getId(), ln);

                    Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());
                    if (payment.getStatus() == PaymentStatus.COMPLETED) {
                        paymentIds.add(payment.getId());
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        Product updated = productRepository.findById(product.getId()).orElseThrow();


        assertThat(updated.getStock()).isEqualTo(0);
        assertThat(paymentIds).hasSize(1);
        assertThat(exceptionCount.get()).isEqualTo(4);
    }

    @Test
    void 재고_부족_상황_동시_차감_테스트() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("동시성테스트유저", 10000));
        Product product = productRepository.save(new Product("상품", 10000, 1, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);

        int threadCount = 5;
        AtomicInteger exceptionCount = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentFacade.processPayment(order.getId(), 10000);
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // then
        List<Payment> byOrderId = paymentRepository.findByOrderId(order.getId());
        Product updated = productRepository.findById(product.getId())
                .orElseThrow();
        assertThat(updated.getStock()).isZero();
        assertThat(byOrderId).hasSize(1);
        assertThat(exceptionCount.get()).isEqualTo(threadCount - 1);
    }

    @Test
    void 환불시_동시_재고증가_검증() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("동시환불유저", 10000));
        Product product = productRepository.save(new Product("상품D", 1000, 5, 1L));
        Order order = orderService.create(
                user.getId(),
                List.of(new OrderLine(product.getId(), 1, product.getPrice()))
        );
        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        int threadCount = 5;
        AtomicInteger exceptionCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentFacade.processRefund(payment.getId());
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // then
        Payment updatedPayment = paymentRepository.findById(payment.getId())
                .orElseThrow();
        Product updatedProduct = productRepository.findById(product.getId())
                .orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.REFUND);
        assertThat(exceptionCount.get()).isEqualTo(threadCount - 1);
        assertThat(updatedProduct.getStock()).isEqualTo(5);
    }

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
        // then
        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        assertThat(payments).hasSize(1);
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
                } catch (Exception e) {
                   e.printStackTrace();
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

        assertThat(updatedUser.getPoint()).isEqualTo(10000);
        assertThat(history).hasSize(2);
    }

}