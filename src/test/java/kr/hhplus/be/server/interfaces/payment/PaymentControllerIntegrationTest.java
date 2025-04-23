package kr.hhplus.be.server.interfaces.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.payment.PaymentFacade;
import kr.hhplus.be.server.domain.coupon.*;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentFacade paymentFacade;
    @Autowired
    private UserCouponRepository userCouponRepository;
    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void cleanDb() {
        productRepository.deleteAll();
        couponRepository.deleteAll();
        userCouponRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }
    private Order createOrder(Long userId, Product product, int quantity) {
        Order order = new Order(userId);
        order.addLine(product.getId(), quantity, product.getPrice());
        return orderService.save(order);
    }

    @Test
    void 결제_성공_쿠폰없이() throws Exception {
        User user = userRepository.save(User.create("결제유저", 50000));
        Product product = productRepository.save(new Product("상품", 25000, 10, 1L));
        Order order = createOrder(user.getId(), product, 2);
        orderRepository.save(order);

        PaymentRequest request = new PaymentRequest(order.getTotalAmount());

        mockMvc.perform(patch("/payments/{orderId}/pay", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.amount", is(50000)));
    }

    @Test
    void 결제_성공_쿠폰_사용됨() throws Exception {
        // given
        User user = userRepository.save(User.create("쿠폰유저", 100000));
        Product product = productRepository.save(new Product("상품", 20000, 10, 1L));
        Coupon coupon = couponRepository.save(Coupon.create("20%할인", 20, 5000, LocalDateTime.now().plusDays(3), 100));
        userCouponRepository.save(UserCoupon.issue(user.getId(), coupon.getId()));

        Order order = createOrder(user.getId(), product, 1);

        orderService.save(order);

        // 실제 할인 금액 = 20000 * 0.2 = 4000원
        int expectedPayAmount = 16000;

        PaymentRequest request = new PaymentRequest(20000);

        mockMvc.perform(patch("/payments/{orderId}/pay", order.getId(), coupon.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(expectedPayAmount))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 유저 포인트 차감 확인
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(100000 - expectedPayAmount);
    }

    @Test
    void 결제_실패_재고부족() throws Exception {
        // given
        User user = userRepository.save(User.create("재고부족유저", 50000));
        Product product = productRepository.save(new Product("상품", 10000, 1, 1L)); // 재고 1
        Order order = createOrder(user.getId(), product, 2);

        PaymentRequest request = new PaymentRequest(20000); // 2 * 10000

        // when & then
        mockMvc.perform(patch("/payments/{orderId}/pay", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("재고가 부족합니다")));
    }

    @Test
    void 결제_실패_포인트부족() throws Exception {
        User user = userRepository.save(User.create("포인트부족", 10000));
        Product product = productRepository.save(new Product("비싼상품", 30000, 10, 1L));
        Order order = createOrder(user.getId(), product, 2);
        orderService.save(order);

        PaymentRequest request = new PaymentRequest(order.getTotalAmount());

        mockMvc.perform(patch("/payments/{orderId}/pay", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("포인트가 부족합니다")));
    }

    @Test
    void 환불_성공_쿠폰없이() throws Exception {
        User user = userRepository.save(User.create("환불유저", 50000));
        Product product = productRepository.save(new Product("상품", 20000, 10, 1L));
        Order order = createOrder(user.getId(), product, 2);
        orderService.save(order);

        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        // 환불 요청
        mockMvc.perform(patch("/payments/{paymentId}/refund", payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId()))
                .andExpect(jsonPath("$.status", is("REFUND")));
    }

    @Test
    void 환불_성공_쿠폰_사용됨_정확한금액() throws Exception {
        // given
        User user = userRepository.save(User.create("환불유저", 100000));
        Product product = productRepository.save(new Product("상품", 20000, 10, 1L));
        Coupon coupon = couponRepository.save(Coupon.create("20%할인", 20, 5000, LocalDateTime.now().plusDays(3), 100));
        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.issue(user.getId(), coupon.getId()));

        Order order = createOrder(user.getId(), product, 1);


        // 결제
        int expectedPayAmount = 16000;
        Payment payment = paymentFacade.processPayment(order.getId(), expectedPayAmount);
        Long paymentId = payment.getId();

        // when - 환불
        mockMvc.perform(patch("/payments/{paymentId}/refund", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUND"));

        // then - 유저 포인트 복구
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(100000);

        // 쿠폰 복구 확인
        UserCoupon refundedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(refundedCoupon.getStatus()).isEqualTo(UserCouponStatus.ISSUED);

        // 재고 복구
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(10);
    }

    @Test
    void 환불_실패_결제되지않은_주문() throws Exception {
        Long invalidPaymentId = 9999L;
        mockMvc.perform(patch("/payments/{paymentId}/refund", invalidPaymentId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("결제를 찾을 수 없습니다")));
    }

}