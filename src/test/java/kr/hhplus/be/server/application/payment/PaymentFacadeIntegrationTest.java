package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.coupon.*;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderLine;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PaymentFacadeIntegrationTest {

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private PaymentRepository paymentRepository;
    @BeforeEach
    void cleanDb() {
        productRepository.deleteAll();
        paymentRepository.deleteAll();
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
    }
    @Test
    void 쿠폰없이_결제_성공시_포인트와_재고가_차감된다() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 30000, 10, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);

        // when
        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getAmount()).isEqualTo(30000);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(70000);

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(9);
    }

    @Test
    void 쿠폰_사용_결제_성공시_쿠폰사용과_포인트재산정된다() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 20000, 10, 1L));
        Coupon coupon = couponRepository.save(
                Coupon.create("20%할인", 20, 5000, LocalDateTime.now().plusDays(1), 10)
        );
        UserCoupon uc = userCouponRepository.save(
                UserCoupon.issue(user.getId(), coupon.getId())
        );
        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);

        // when
        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getCouponId()).isEqualTo(coupon.getId());
        assertThat(payment.getAmount()).isEqualTo(16000);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(84000);

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(9);

        UserCoupon updatedUc = userCouponRepository.findById(uc.getId()).orElseThrow();
        assertThat(updatedUc.getStatus()).isEqualTo(UserCouponStatus.USED);
    }

    @Test
    void 결제_후_환불_정상시_재고_포인트_복원된다() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 30000, 10, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);
        Payment pay = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        // when
        Payment refund = paymentFacade.processRefund(pay.getId());

        // then
        assertThat(refund.getStatus()).isEqualTo(PaymentStatus.REFUND);
        assertThat(userRepository.findById(user.getId()).get().getPoint())
                .isEqualTo(100000);
        assertThat(productRepository.findById(product.getId()).get().getStock())
                .isEqualTo(10);
    }

    @Test
    void 재고부족_결제시_IllegalStateException_발생() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 1, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 2, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(order.getId(), order.getTotalAmount())
        );
    }

    @Test
    void 포인트부족_결제시_IllegalStateException_발생() {
        // given
        User user = userRepository.save(User.create("포인트부족", 5000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(order.getId(), order.getTotalAmount())
        );

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(10);
    }

    @Test
    void 결제미완료_환불시_IllegalStateException_발생() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);
        Payment pending = paymentRepository.save(
                Payment.withoutCoupon(order.getId(), order.getTotalAmount())
        );

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentFacade.processRefund(pending.getId())
        );
    }

    @Test
    void 이미환불된_결제_환불시_IllegalStateException_발생() {
        // given
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 1, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);
        Payment pay = paymentRepository.save(
                Payment.withoutCoupon(order.getId(), order.getTotalAmount())
        );
        pay.complete();
        pay.refund();
        paymentRepository.save(pay);

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentFacade.processRefund(pay.getId())
        );
    }
}
