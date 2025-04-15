package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.coupon.UserCouponService;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.user.User;
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
public class PaymentFacadeTest {

    @InjectMocks private PaymentFacade paymentFacade;
    @Mock private OrderService orderService;
    @Mock private ProductService productService;
    @Mock private UserPointFacade userPointFacade;
    @Mock private PaymentService paymentService;
    @Mock private UserCouponService userCouponService;
    @Mock private CouponService couponService;

    @Test
    void 결제_정상_쿠폰없음() {
        // given
        Long orderId = 1L;
        int paymentAmount = 25000;
        Long userId = 10L;
        OrderItem item = OrderItem.create(mock(Order.class), 101L, 5, 5000);
        OrderItemCommand command = new OrderItemCommand(101L, 1, 10000);
        Order dummyOrder = Order.create(userId, List.of(command));

        dummyOrder.updateItems(List.of(item));

        when(orderService.getOrderOrThrow(orderId)).thenReturn(dummyOrder);
        when(productService.decreaseStock(101L, 5)).thenReturn(mock(Product.class));
        when(userCouponService.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        when(userPointFacade.usePoint(userId, paymentAmount)).thenReturn(mock(User.class));
        when(orderService.payOrder(orderId)).thenReturn(dummyOrder);

        // 결제 초기화 및 완료 처리
        Payment completed = Payment.withoutCoupon(orderId, paymentAmount);
        completed.complete();

        when(paymentService.initiateWithoutCoupon(orderId, paymentAmount)).thenReturn(completed);
        when(paymentService.completePayment(completed.getId())).thenReturn(completed);

        // when
        Payment result = paymentFacade.processPayment(orderId, paymentAmount);

        // then
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals(orderId, result.getOrderId());
        assertEquals(paymentAmount, result.getAmount());
        assertNull(result.getCouponId());

        verify(orderService).getOrderOrThrow(orderId);
        verify(productService).decreaseStock(101L, 5);
        verify(userCouponService).findByUserId(userId);
        verify(userPointFacade).usePoint(userId, paymentAmount);
        verify(orderService).payOrder(orderId);
        verify(paymentService).initiateWithoutCoupon(orderId, paymentAmount);
        verify(paymentService).completePayment(completed.getId());
    }

    @Test
    void 결제_정상_쿠폰사용() {
        // given
        Long orderId = 1L;
        Long userId = 10L;
        Long couponId = 99L;
        int paymentAmount = 10000;
        int discount = 2000;
        int finalAmount = paymentAmount - discount;

        // 더미 주문과 주문 항목 설정
        OrderItem item = OrderItem.create(mock(Order.class), 101L, 1, 10000);
        OrderItemCommand command = new OrderItemCommand(101L, 1, 10000);
        Order dummyOrder = Order.create(userId, List.of(command));

        when(orderService.getOrderOrThrow(orderId)).thenReturn(dummyOrder);
        when(productService.decreaseStock(101L, 1)).thenReturn(mock(Product.class));

        UserCoupon userCoupon = mock(UserCoupon.class);
        when(userCoupon.getCouponId()).thenReturn(couponId);
        when(userCouponService.findByUserId(userId)).thenReturn(List.of(userCoupon));
        when(userCouponService.useCoupon(couponId)).thenReturn(userCoupon);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .name("테스트쿠폰")
                .discountRate(20)
                .maxDiscountAmount(2000)
                .status(CouponStatus.ACTIVE)
                .expirationAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .limitCount(100)
                .issuedCount(1)
                .build();
        when(couponService.getCouponOrThrow(couponId)).thenReturn(coupon);

        when(userPointFacade.usePoint(userId, finalAmount)).thenReturn(mock(User.class));
        when(orderService.payOrder(orderId)).thenReturn(dummyOrder);

        Payment completed = Payment.withCoupon(orderId, finalAmount, couponId);
        completed.complete();

        when(paymentService.initiateWithCoupon(orderId, finalAmount, couponId)).thenReturn(completed);
        when(paymentService.completePayment(completed.getId())).thenReturn(completed);

        // when
        Payment result = paymentFacade.processPayment(orderId, paymentAmount);

        // then
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals(finalAmount, result.getAmount());
        assertEquals(couponId, result.getCouponId());

        verify(productService).decreaseStock(101L, 1);
        verify(userCouponService).useCoupon(couponId);
        verify(couponService).getCouponOrThrow(couponId);
        verify(userPointFacade).usePoint(userId, finalAmount);
        verify(orderService).payOrder(orderId);
        verify(paymentService).completePayment(completed.getId());
    }

    @Test
    void 결제_실패_재고_부족() {
        Long orderId = 1L;
        Long userId = 10L;
        int paymentAmount = 10000;

        OrderItem orderItem = OrderItem.create(mock(Order.class), 101L, 1, paymentAmount);
        OrderItemCommand command = new OrderItemCommand(101L, 1, 10000);
        Order dummyOrder = Order.create(userId, List.of(command));

        dummyOrder.updateItems(List.of(orderItem));
        when(orderService.getOrderOrThrow(orderId)).thenReturn(dummyOrder);

        // stock 차감 시 예외 발생
        doThrow(new IllegalStateException("상품 재고가 부족합니다."))
                .when(productService).decreaseStock(101L, 1);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            paymentFacade.processPayment(orderId, 10000);
        });

        assertEquals("상품 재고가 부족합니다.", e.getMessage());
    }

    @Test
    void 결제_실패_포인트부족() {
        Long orderId = 1L;
        Long userId = 10L;
        int paymentAmount = 10000;

        OrderItem orderItem = OrderItem.create(mock(Order.class), 101L, 1, paymentAmount);
        OrderItemCommand command = new OrderItemCommand(101L, 1, 10000);
        Order dummyOrder = Order.create(userId, List.of(command));

        when(orderService.getOrderOrThrow(orderId)).thenReturn(dummyOrder);
        when(userCouponService.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(productService.decreaseStock(101L, 1)).thenReturn(mock(Product.class));

        when(userPointFacade.usePoint(userId, paymentAmount))
                .thenThrow(new IllegalStateException("포인트 부족"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, paymentAmount));
        assertEquals("포인트 부족", ex.getMessage());
    }

    @Test
    void 환불_정상처리() {
        Long orderId = 1L;
        Long userId = 10L;
        int paymentAmount = 10000;
        OrderItem orderItem = OrderItem.create(mock(Order.class), 101L, 1, paymentAmount);
        Order dummyOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .totalAmount(paymentAmount)
                .status(OrderStatus.PAID)
                .items(List.of(orderItem))
                .build();

        when(orderService.getOrderOrThrow(orderId)).thenReturn(dummyOrder);
        when(productService.increaseStock(101L, 1)).thenReturn(mock(Product.class));
        when(userPointFacade.refundPoint(userId, paymentAmount, orderId)).thenReturn(mock(User.class));

        Payment refunded = Payment.withoutCoupon(orderId, paymentAmount);
        refunded.complete();
        refunded.refund();

        when(paymentService.refundPayment(orderId)).thenReturn(refunded);

        Payment result = paymentFacade.processRefund(orderId);

        assertEquals(PaymentStatus.REFUND, result.getStatus());
        verify(paymentService).refundPayment(orderId);
    }

    @Test
    void 결제상태가_대기일때_환불_실패() {
        // given
        Long orderId = 1L;
        Long userId = 10L;
        int paymentAmount = 10000;

        OrderItem item = OrderItem.create(mock(Order.class), 101L, 2, 5000);
        OrderItemCommand command = new OrderItemCommand(101L, 1, 10000);
        Order dummyOrder = Order.create(userId, List.of(command));

        Payment pendingPayment = Payment.withoutCoupon(orderId, paymentAmount);  // 상태는 PENDING

        when(orderService.getOrderOrThrow(orderId)).thenReturn(dummyOrder);
        when(paymentService.refundPayment(orderId)).thenAnswer(invocation -> {
            pendingPayment.refund(); // 여기서 예외 발생
            return pendingPayment;
        });

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            paymentFacade.processRefund(orderId); // 내부에서 refund() 호출됨 → 예외 발생
        });

        assertEquals("결제가 완료되지 않은 주문입니다.", e.getMessage());
    }

}