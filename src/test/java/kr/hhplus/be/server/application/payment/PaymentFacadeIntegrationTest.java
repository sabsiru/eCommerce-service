package kr.hhplus.be.server.application.payment;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.domain.coupon.*;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

@SpringBootTest
@Transactional
class PaymentFacadeIntegrationTest {

    @Autowired
    PaymentFacade paymentFacade;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    CouponRepository couponRepository;
    @Autowired
    UserCouponRepository userCouponRepository;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    OrderItemRepository orderItemRepository;
    @Autowired
    OrderService orderService;


    @Test
    void 쿠폰없이_결제_성공() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 30000, 10, 1L));

        List<OrderItemCommand> commands = List.of(
                new OrderItemCommand(product.getId(), 1, product.getPrice())
        );
        Order order = Order.create(user.getId(), commands);
        orderService.save(order);

        // when
        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount()); // 쿠폰 없음

        paymentRepository.flush(); // ← 또는 em.flush()
        // then
        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getAmount()).isEqualTo(30000);

        // 유저 포인트 차감 확인
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(70000);

        // 상품 재고 차감 확인
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(9);
    }

    @Test
    void 쿠폰_사용_결제_성공() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 20000, 10, 1L));

        // 할인율 20%, 최대 할인 5000
        Coupon coupon = couponRepository.save(Coupon.create("20%할인", 20, 5000, LocalDateTime.now().plusDays(1), 10));
        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.issue(user.getId(), coupon.getId()));

        List<OrderItemCommand> items = List.of(new OrderItemCommand(product.getId(), 1, 20000));
        Order order = Order.create(user.getId(), items);
        orderService.save(order);

        // when
        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        // then
        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getCouponId()).isEqualTo(coupon.getId());
        assertThat(payment.getAmount()).isEqualTo(16000); // 20000 - 4000 할인

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(84000); // 100000 - 16000

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(9);

        UserCoupon updatedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
    }

    @Test
    void 쿠폰없이_결제_후_환불_성공() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 30000, 10, 1L));

        Order order = Order.create(user.getId(), List.of(
                new OrderItemCommand(product.getId(), 1, product.getPrice())
        ));
        orderService.save(order);

        // 결제 수행 (쿠폰 없음)
        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        // when: 환불 요청
        Payment refunded = paymentFacade.processRefund(payment.getId());

        // then
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUND);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(100000);  // 복원됨

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(10);  // 복원됨
    }

    @Test
    void 쿠폰_사용_결제_후_환불_성공() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 20000, 10, 1L));

        Coupon coupon = couponRepository.save(Coupon.create("20%할인", 20, 5000, LocalDateTime.now().plusDays(1), 10));
        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.issue(user.getId(), coupon.getId()));

        Order order = Order.create(user.getId(), List.of(
                new OrderItemCommand(product.getId(), 1, 20000)
        ));
        orderService.save(order);

        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        // when
        Payment refunded = paymentFacade.processRefund(payment.getId());

        // then
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUND);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(100000); // 포인트 복원

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(10);  // 재고 복원

        UserCoupon updatedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStatus()).isEqualTo(UserCouponStatus.ISSUED); // 쿠폰 복원
    }

    @Test
    void 결제_실패_재고부족() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 1, 1L)); // 재고 1

        Order order = Order.create(user.getId(), List.of(
                new OrderItemCommand(product.getId(), 2, 10000) // 수량 2 → 초과
        ));
        orderService.save(order);
        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(order.getId(), 20000));

        assertThat(e.getMessage()).contains("재고가 부족합니다");
    }

    @Test
    void 결제_실패_포인트부족() {
        // given
        User user = userRepository.save(User.create("포인트부족", 5000)); // 부족한 포인트
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        Order order = Order.create(user.getId(), List.of(
                new OrderItemCommand(product.getId(), 1, 10000)
        ));
        orderService.save(order);
        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(order.getId(), 10000));

        assertThat(e.getMessage()).contains("포인트가 부족합니다");
    }

    @Test
    void 환불_실패_결제_미완료() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        Order order = Order.create(user.getId(), List.of(
                new OrderItemCommand(product.getId(), 1, product.getPrice())
        ));
        orderService.save(order);
        // 직접 결제 저장 (PENDING 상태 유지)
        Payment pendingPayment = paymentRepository.save(Payment.withoutCoupon(order.getId(), 10000));

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processRefund(pendingPayment.getId()));

        assertThat(e.getMessage()).isEqualTo("결제가 완료되지 않은 주문입니다.");
    }

    @Test
    void 환불_실패_이미_환불된_결제() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        Order order = Order.create(user.getId(), List.of(
                new OrderItemCommand(product.getId(), 1, product.getPrice())
        ));
        orderService.save(order);
        // 결제 완료 후 상태 수동 변경
        Payment payment = paymentRepository.save(Payment.withoutCoupon(order.getId(), 10000));
        payment.complete();
        payment.refund(); // 상태를 REFUND로 변경
        paymentRepository.save(payment);

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processRefund(payment.getId()));

        assertThat(e.getMessage()).isEqualTo("결제가 완료되지 않은 주문입니다.");
    }
}
