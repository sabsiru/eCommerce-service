package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentService;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @InjectMocks
    private PaymentFacade paymentFacade;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private UserPointFacade userPointFacade;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CouponService couponService;

    private final Long orderId = 1L;
    private final Long userId = 100L;
    private final Long productId = 101L;
    private final int quantity = 2;
    private final int unitPrice = 15000;
    private final int totalAmount = quantity * unitPrice;

    private Order order;

    @BeforeEach
    void setUp() {
        // Order 생성 및 OrderLine 추가
        order = new Order(userId);
        order.addLine(productId, quantity, unitPrice);
    }

    @Test
    void 결제_정상_쿠폰없음() {
        when(orderService.getOrderOrThrowPaid(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(order.getItems());
        when(couponService.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(productService.decreaseStock(productId, quantity)).thenReturn(mock(Product.class));
        when(userPointFacade.usePoint(userId, totalAmount)).thenReturn(mock(User.class));
        when(orderService.pay(orderId)).thenReturn(order);

        Payment mockPayment = Payment.withoutCoupon(orderId, totalAmount);
        mockPayment.complete();
        when(paymentService.initiateWithoutCoupon(orderId, totalAmount)).thenReturn(mockPayment);
        when(paymentService.complete(mockPayment.getId())).thenReturn(mockPayment);

        Payment result = paymentFacade.processPayment(orderId, totalAmount);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals(totalAmount, result.getAmount());
        assertNull(result.getCouponId());

        verify(couponService).findByUserId(userId);
        verify(productService).decreaseStock(productId, quantity);
        verify(userPointFacade).usePoint(userId, totalAmount);
        verify(orderService).pay(orderId);
        verify(paymentService).initiateWithoutCoupon(orderId, totalAmount);
        verify(paymentService).complete(mockPayment.getId());
    }

    @Test
    void 결제_정상_쿠폰사용() {
        Long couponId = 99L;
        int discount = 2000;
        int finalAmount = totalAmount - discount;
        Coupon coupon = Coupon.create("test", 30, discount, LocalDateTime.now().plusDays(1), 10);
        UserCoupon uc = UserCoupon.issue(userId, couponId);

        when(orderService.getOrderOrThrowPaid(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(order.getItems());
        when(couponService.findByUserId(userId)).thenReturn(List.of(uc));
        when(couponService.getCouponOrThrow(couponId)).thenReturn(coupon);
        when(couponService.use(couponId)).thenReturn(uc);
        when(productService.decreaseStock(productId, quantity)).thenReturn(mock(Product.class));
        when(userPointFacade.usePoint(userId, finalAmount)).thenReturn(mock(User.class));
        when(orderService.pay(orderId)).thenReturn(order);

        Payment mockPayment = Payment.withCoupon(orderId, finalAmount, couponId);
        mockPayment.complete();
        when(paymentService.initiateWithCoupon(orderId, finalAmount, couponId)).thenReturn(mockPayment);
        when(paymentService.complete(mockPayment.getId())).thenReturn(mockPayment);

        Payment result = paymentFacade.processPayment(orderId, totalAmount);

        assertNotNull(result);
        assertEquals(finalAmount, result.getAmount());
        assertEquals(couponId, result.getCouponId());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());

        verify(couponService).findByUserId(userId);
        verify(couponService).getCouponOrThrow(couponId);
        verify(couponService).use(couponId);
    }

    @Test
    void 결제_실패_재고_부족() {
        when(orderService.getOrderOrThrowPaid(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(order.getItems());
        doThrow(new IllegalStateException("상품 재고가 부족합니다.")).when(productService).decreaseStock(productId, quantity);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, totalAmount));
        assertEquals("상품 재고가 부족합니다.", e.getMessage());
    }

    @Test
    void 결제_실패_포인트부족() {
        when(orderService.getOrderOrThrowPaid(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(order.getItems());
        when(couponService.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(productService.decreaseStock(productId, quantity)).thenReturn(mock(Product.class));
        doThrow(new IllegalStateException("포인트 부족")).when(userPointFacade).usePoint(userId, totalAmount);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, totalAmount));
        assertEquals("포인트 부족", e.getMessage());
    }

    @Test
    void 환불_정상처리() {
        Payment refunded = Payment.withoutCoupon(orderId, totalAmount);
        refunded.complete();
        refunded.refund();
        Long paymentId = refunded.getId();  // ★ paymentId를 꺼냅니다.

        when(orderService.getOrderOrThrowCancel(orderId)).thenReturn(order);
        // ↓ 새로운 stub: 이 리스트를 돌면서 increaseStock 호출이 일어나야 합니다
        List<OrderItem> dummyItems = List.of(
                new OrderItem(order, productId, quantity, /*orderPrice*/ 10000)
        );

        when(orderService.getOrderItems(orderId)).thenReturn(dummyItems);
        when(productService.increaseStock(productId, quantity)).thenReturn(mock(Product.class));
        when(userPointFacade.refundPoint(userId, totalAmount, paymentId)).thenReturn(mock(User.class));
        when(paymentService.refund(paymentId)).thenReturn(refunded);

        // when
        Payment result = paymentFacade.processRefund(paymentId);

        // then
        assertEquals(PaymentStatus.REFUND, result.getStatus());

        verify(paymentService).refund(paymentId);
        verify(userPointFacade).refundPoint(userId, totalAmount, paymentId);
        verify(productService).increaseStock(productId, quantity);
    }

    @Test
    void 환불_실패_결제미완료일때() {
        // given: paymentService.refundPayment 호출 시 결제 미완료 예외 던지기
        when(paymentService.refund(orderId))
                .thenThrow(new IllegalStateException("결제가 완료되지 않은 주문입니다."));

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processRefund(orderId));

        assertEquals("결제가 완료되지 않은 주문입니다.", e.getMessage());
    }
}
