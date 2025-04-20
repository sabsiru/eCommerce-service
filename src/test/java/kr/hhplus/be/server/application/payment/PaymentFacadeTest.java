package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCouponService;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentService;
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
        Long userId = 100L;
        Long productId = 101L;
        int quantity = 2;
        int unitPrice = 15000;
        int totalAmount = quantity * unitPrice;

        // 주문 항목
        OrderItem orderItem = OrderItem.builder()
                .id(1000L)
                .productId(productId)
                .quantity(quantity)
                .orderPrice(unitPrice)
                .createdAt(LocalDateTime.now())
                .build();

        // 주문 객체
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .items(List.of(orderItem))
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderItem.setOrder(order);

        // stubbing
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(List.of(orderItem));
        when(productService.decreaseStock(productId, quantity)).thenReturn(mock(Product.class));
        when(userCouponService.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(userPointFacade.usePoint(userId, totalAmount)).thenReturn(mock(User.class));
        when(orderService.pay(orderId)).thenReturn(order);

        Payment expected = Payment.withoutCoupon(orderId, totalAmount);
        expected.complete();

        when(paymentService.initiateWithoutCoupon(orderId, totalAmount)).thenReturn(expected);
        when(paymentService.completePayment(expected.getId())).thenReturn(expected);

        // when
        Payment result = paymentFacade.processPayment(orderId, totalAmount);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals(totalAmount, result.getAmount());
        assertNull(result.getCouponId());

        // verify
        verify(productService).decreaseStock(productId, quantity);
        verify(userPointFacade).usePoint(userId, totalAmount);
        verify(orderService).pay(orderId);
        verify(paymentService).initiateWithoutCoupon(orderId, totalAmount);
        verify(paymentService).completePayment(expected.getId());
    }

    @Test
    void 결제_정상_쿠폰사용() {
        // given
        Long orderId = 1L;
        Long userId = 10L;
        Long couponId = 99L;
        Long productId = 101L;
        int quantity = 1;
        int unitPrice = 10000;
        int discount = 2000;
        int paymentAmount = quantity * unitPrice;
        int finalAmount = paymentAmount - discount;

        // 실제 OrderItem 생성
        OrderItem item = OrderItem.builder()
                .id(1000L)
                .productId(productId)
                .quantity(quantity)
                .orderPrice(unitPrice)
                .createdAt(LocalDateTime.now())
                .build();

        // 실제 Order 생성
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .items(List.of(item))
                .totalAmount(paymentAmount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        item.setOrder(order); // 연관관계 연결

        // stubbing
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(List.of(item));
        when(productService.decreaseStock(productId, quantity)).thenReturn(mock(Product.class));

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .name("테스트쿠폰")
                .discountRate(20)
                .maxDiscountAmount(discount)
                .status(CouponStatus.ACTIVE)
                .expirationAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .limitCount(100)
                .issuedCount(1)
                .build();
        when(couponService.getCouponOrThrow(couponId)).thenReturn(coupon);

        UserCoupon userCoupon = mock(UserCoupon.class);
        when(userCoupon.getCouponId()).thenReturn(couponId);
        when(userCouponService.findByUserId(userId)).thenReturn(List.of(userCoupon));
        when(userCouponService.useCoupon(couponId)).thenReturn(userCoupon);

        when(userPointFacade.usePoint(userId, finalAmount)).thenReturn(mock(User.class));
        when(orderService.pay(orderId)).thenReturn(order);

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

        // verify (핵심 상호작용만)
        verify(productService).decreaseStock(productId, quantity);
        verify(userCouponService).useCoupon(couponId);
        verify(userPointFacade).usePoint(userId, finalAmount);
        verify(orderService).pay(orderId);
        verify(paymentService).completePayment(completed.getId());
    }

    @Test
    void 결제_실패_재고_부족() {
        // given
        Long orderId = 1L;
        Long userId = 10L;
        Long productId = 101L;
        int quantity = 1;
        int unitPrice = 10000;
        int paymentAmount = quantity * unitPrice;

        // 실제 OrderItem
        OrderItem item = OrderItem.builder()
                .id(1000L)
                .productId(productId)
                .quantity(quantity)
                .orderPrice(unitPrice)
                .createdAt(LocalDateTime.now())
                .build();

        // 실제 Order
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .items(List.of(item))
                .totalAmount(paymentAmount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        item.setOrder(order);

        // 스텁 설정
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(List.of(item));

        // 예외 발생 설정 (재고 부족)
        doThrow(new IllegalStateException("상품 재고가 부족합니다."))
                .when(productService).decreaseStock(productId, quantity);

        // when
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            paymentFacade.processPayment(orderId, paymentAmount);
        });

        // then
        assertEquals("상품 재고가 부족합니다.", e.getMessage());

        // 핵심 검증만 유지
        verify(productService).decreaseStock(productId, quantity);
    }

    @Test
    void 결제_실패_포인트부족() {
        // given
        Long orderId = 1L;
        Long userId = 10L;
        Long productId = 101L;
        int quantity = 1;
        int unitPrice = 10000;
        int paymentAmount = quantity * unitPrice;

        // 실제 OrderItem
        OrderItem item = OrderItem.builder()
                .id(1000L)
                .productId(productId)
                .quantity(quantity)
                .orderPrice(unitPrice)
                .createdAt(LocalDateTime.now())
                .build();

        // 실제 Order
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .items(List.of(item))
                .totalAmount(paymentAmount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        item.setOrder(order);

        // stubbing
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);
        when(orderService.getOrderItems(orderId)).thenReturn(List.of(item));
        when(userCouponService.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(productService.decreaseStock(productId, quantity)).thenReturn(mock(Product.class));
        when(userPointFacade.usePoint(userId, paymentAmount))
                .thenThrow(new IllegalStateException("포인트 부족"));

        // when
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, paymentAmount));

        // then
        assertEquals("포인트 부족", ex.getMessage());

        // 핵심 상호작용만 확인
        verify(productService).decreaseStock(productId, quantity);
        verify(userPointFacade).usePoint(userId, paymentAmount);
    }

    @Test
    void 환불_정상처리() {
        // given
        Long orderId = 1L;
        Long userId = 10L;
        Long productId = 101L;
        int quantity = 1;
        int unitPrice = 10000;
        int paymentAmount = quantity * unitPrice;

        // 1. 실제 OrderItem 생성
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(productId)
                .quantity(quantity)
                .orderPrice(unitPrice)
                .createdAt(LocalDateTime.now())
                .build();

        // 2. Order 객체 생성 및 연관관계 설정
        Order dummyOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PAID)
                .totalAmount(paymentAmount)
                .items(List.of(item))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        item.setOrder(dummyOrder);

        // 3. 환불 대상 결제 객체 생성
        Payment refunded = Payment.withoutCoupon(orderId, paymentAmount);
        refunded.complete();
        refunded.refund();

        // 4. Stubbing
        when(orderService.getOrderOrThrow(orderId)).thenReturn(dummyOrder);
        when(orderService.getOrderItems(orderId)).thenReturn(List.of(item));
        when(paymentService.refundPayment(orderId)).thenReturn(refunded);
        when(userPointFacade.refundPoint(userId, paymentAmount, orderId)).thenReturn(mock(User.class));
        when(productService.increaseStock(productId, quantity)).thenReturn(mock(Product.class));

        // when
        Payment result = paymentFacade.processRefund(orderId);

        // then
        assertEquals(PaymentStatus.REFUND, result.getStatus());

        // 핵심 상호작용 검증
        verify(paymentService).refundPayment(orderId);
        verify(userPointFacade).refundPoint(userId, paymentAmount, orderId);
        verify(productService).increaseStock(productId, quantity);
    }

    @Test
    void 결제상태가_대기일때_환불_실패() {
        // given
        Long orderId = 1L;
        int paymentAmount = 10000;

        Payment pendingPayment = Payment.withoutCoupon(orderId, paymentAmount);  // 상태는 PENDING

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