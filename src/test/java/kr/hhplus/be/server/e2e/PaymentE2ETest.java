package kr.hhplus.be.server.e2e;

import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import kr.hhplus.be.server.interfaces.payment.PaymentRequest;
import kr.hhplus.be.server.interfaces.payment.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PaymentE2ETest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 주문_생성_후_결제까지_정상_흐름() {

        User user = userRepository.save(User.create("e2e-user", 10000));
        Product product = productRepository.save(new Product("상품", 5000, 10,1L));


        OrderRequest orderRequest = new OrderRequest(
                user.getId(),
                List.of(new OrderRequest.Item(product.getId(), 2, 5000))  // 총액: 10000
        );

        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity(
                "/orders", orderRequest, OrderResponse.class
        );

        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long orderId = orderRes.getBody().getId();
        PaymentRequest req = new PaymentRequest(10000);
        HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(req);

        ResponseEntity<PaymentResponse> paymentRes = restTemplate.exchange(
                "/payments/" + orderId + "/pay",
                HttpMethod.PATCH,
                httpEntity,
                PaymentResponse.class
        );

        assertThat(paymentRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paymentRes.getBody().getStatus()).isEqualTo("COMPLETED");


        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(0);
    }

    @Test
    void 결제_후_환불_정상_흐름_검증() {
        // given
        Product product = productRepository.save(new Product("환불가능 상품", 5000, 5, 1L));
        User user = userRepository.save(User.create("환불유저", 10000));

        OrderCommand.Create command = new OrderCommand.Create(
                user.getId(), List.of(new OrderCommand.Item(product.getId(), 1, 5000))
        );

        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity(
                "/orders",command,OrderResponse.class
        );
        assertThat(orderRes.getStatusCode().is2xxSuccessful()).isTrue();
        Long orderId = orderRes.getBody().getId();

        HttpEntity<PaymentRequest> payReq = new HttpEntity<>(new PaymentRequest(5000));
        ResponseEntity<String> payRes = restTemplate.exchange(
                "/payments/" + orderId + "/pay",
                HttpMethod.PATCH,
                payReq,
                String.class
        );

        User afterPayment = userRepository.findById(user.getId()).orElseThrow();
        assertThat(afterPayment.getPoint()).isEqualTo(5000);
        assertThat(payRes.getStatusCode().is2xxSuccessful()).isTrue();

        // 환불 요청
        ResponseEntity<String> refundRes = restTemplate.exchange(
                "/payments/" + orderId + "/refund",
                HttpMethod.PATCH,
                null,
                String.class
        );
        User afterRefund = userRepository.findById(user.getId()).orElseThrow();
        assertThat(afterRefund.getPoint()).isEqualTo(10000);
        assertThat(refundRes.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void 쿠폰_사용_결제_후_환불_정상_흐름_및_포인트_환불금액_검증() {
        // given
        User user = userRepository.save(User.create("쿠폰유저", 10000));
        Product product = productRepository.save(new Product("쿠폰상품", 5000, 5, 1L));
        Coupon coupon = couponRepository.save(Coupon.create("10% 할인쿠폰", 10, 1000, LocalDateTime.now().plusDays(1),100));
        userCouponRepository.save(UserCoupon.issue(user.getId(), coupon.getId()));

        // 주문 생성
        OrderCommand.Create command = new OrderCommand.Create(
                user.getId(), List.of(new OrderCommand.Item(product.getId(), 1, 5000))
        );
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity(
                "/orders", command, OrderResponse.class);
        assertThat(orderRes.getStatusCode().is2xxSuccessful()).isTrue();
        Long orderId = orderRes.getBody().getId();

        HttpEntity<PaymentRequest> payReq = new HttpEntity<>(new PaymentRequest(5000));
        ResponseEntity<String> payRes = restTemplate.exchange(
                "/payments/" + orderId + "/pay",
                HttpMethod.PATCH,
                payReq,
                String.class
        );
        assertThat(payRes.getStatusCode().is2xxSuccessful()).isTrue();

        User afterPayment = userRepository.findById(user.getId()).orElseThrow();
        assertThat(afterPayment.getPoint()).isEqualTo(5500);

        ResponseEntity<String> refundRes = restTemplate.exchange(
                "/payments/" + orderId + "/refund",
                HttpMethod.PATCH,
                null,
                String.class
        );
        assertThat(refundRes.getStatusCode().is2xxSuccessful()).isTrue();

        User afterRefund = userRepository.findById(user.getId()).orElseThrow();
        assertThat(afterRefund.getPoint()).isEqualTo(10000);
    }
}
