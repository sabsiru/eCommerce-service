package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.payment.event.PaymentEventPublisher;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.event.CouponEventPublisher;
import kr.hhplus.be.server.domain.coupon.event.CouponValidateEvent;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentService;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.point.event.PointEventPublisher;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private PaymentEventPublisher paymentEventPublisher;
    @Mock
    private CouponEventPublisher couponEventPublisher;
    @Mock
    private PointEventPublisher pointEventPublisher;

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
        when(couponService.getAvailableCouponId(order.getUserId())).thenReturn(null);
        when(orderService.pay(orderId)).thenReturn(order);

        Payment mockPayment = Payment.create(orderId, totalAmount, null);
        when(paymentService.create(orderId, totalAmount, null)).thenReturn(mockPayment);

        Payment result = paymentFacade.processPayment(orderId, totalAmount);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals(totalAmount, result.getAmount());
        assertNull(result.getCouponId());

        verify(pointEventPublisher).publishPointUsed(any());
        verify(paymentEventPublisher).publishPaymentCompleted(eq(mockPayment), eq(order));
        verify(orderService).pay(orderId);
        verify(paymentService).create(orderId, totalAmount, null);
    }

    @Test
    void 결제_정상_쿠폰사용() {
        // given
        Long couponId = 99L;
        int discount = 2000;
        int discountedAmount = totalAmount - discount;

        when(orderService.getOrderOrThrowPaid(orderId)).thenReturn(order);
        when(couponService.getAvailableCouponId(order.getUserId())).thenReturn(couponId);

        when(couponService.calculateDiscountAmount(couponId, totalAmount)).thenReturn(discount);

        when(orderService.pay(orderId)).thenReturn(order);

        Payment mockPayment = Payment.create(orderId, discountedAmount, couponId);
        when(paymentService.create(orderId, discountedAmount, couponId)).thenReturn(mockPayment);

        // when
        Payment result = paymentFacade.processPayment(orderId, totalAmount);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(couponId, result.getCouponId());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals(discountedAmount, result.getAmount());

        verify(couponEventPublisher).publishCouponValidate(any(CouponValidateEvent.class));
        verify(paymentEventPublisher).publishPaymentCompleted(eq(mockPayment), eq(order));
        verify(orderService).pay(orderId);
        verify(couponService).calculateDiscountAmount(couponId, totalAmount);
        verify(paymentService).create(orderId, discountedAmount, couponId);
    }

@Test
void 결제_실패_재고_부족() {
    // given
    when(orderService.getOrderOrThrowPaid(orderId)).thenReturn(order);
    when(couponService.getAvailableCouponId(order.getUserId())).thenReturn(null);

    Payment mockPayment = Payment.create(orderId, totalAmount, null);
    when(paymentService.create(orderId, totalAmount, null)).thenReturn(mockPayment);

    when(orderService.pay(orderId)).thenReturn(order);

    doAnswer(invocation -> {
        throw new IllegalStateException("상품 재고가 부족합니다.");
    }).when(paymentEventPublisher).publishPaymentCompleted(any(), any());

    // when & then
    IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> paymentFacade.processPayment(orderId, totalAmount));
    assertEquals("상품 재고가 부족합니다.", e.getMessage());

    // PaymentCompletedEvent
    verify(paymentEventPublisher).publishPaymentCompleted(any(), any());
}

    @Test
    void 결제_실패_포인트부족_쿠폰없음() {
        // given
        when(orderService.getOrderOrThrowPaid(orderId)).thenReturn(order);
        when(couponService.getAvailableCouponId(order.getUserId())).thenReturn(null);

        doAnswer(invocation -> {
            throw new IllegalStateException("포인트 부족");
        }).when(pointEventPublisher).publishPointUsed(any());

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, totalAmount));
        assertEquals("포인트 부족", e.getMessage());

        verify(pointEventPublisher).publishPointUsed(any());
    }

    @Test
    void 환불_정상처리() {
        Payment refunded = Payment.withoutCoupon(orderId, totalAmount);
        refunded.complete();
        refunded.refund();
        Long paymentId = refunded.getId();

        when(orderService.getOrderOrThrowCancel(orderId)).thenReturn(order);
        List<OrderItem> dummyItems = List.of(
                new OrderItem(order, productId, quantity, 10000)
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
        // given
        when(paymentService.refund(orderId))
                .thenThrow(new IllegalStateException("결제가 완료되지 않은 주문입니다."));

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processRefund(orderId));

        assertEquals("결제가 완료되지 않은 주문입니다.", e.getMessage());
    }

}
