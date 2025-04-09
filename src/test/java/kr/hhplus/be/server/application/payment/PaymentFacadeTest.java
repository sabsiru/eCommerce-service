package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointFacade;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentFacadeTest {

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

    @Test
    void 결제_정상_작동() {
        // Arrange
        Long orderId = 1L;
        int paymentAmount = 100_000;
        int usePointAmount = 10_000;

        LocalDateTime now = LocalDateTime.now();
        // Order 객체 생성 (order.items에 1개 주문 항목 있음)
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now);  // 5 * 5000 = 25000
        Order order = new Order(orderId, 1L, List.of(orderItem), 25000, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // productService.decreaseStock()가 Product 객체를 반환하도록 Stub 처리
        Product dummyProduct = new Product(101L, "Dummy Product", 5000, 45, 1L, LocalDateTime.now(), LocalDateTime.now());
        when(productService.decreaseStock(101L, 5)).thenReturn(dummyProduct);

        // userPointFacade.usePoint()가 업데이트된 User 객체를 반환하도록 Stub 처리
        // 가정: User domain record: id, name, point, createdAt, updatedAt
        User updatedUser = new User(order.userId(), "TestUser", 40000, now, now);
        when(userPointFacade.usePoint(order.userId(), usePointAmount)).thenReturn(updatedUser);

        // orderService.payOrder()를 통해 주문 상태 PAID로 변경된 Order 반환
        Order paidOrder = new Order(orderId, order.userId(), order.items(), order.totalAmount(), OrderStatus.PAID, now, now.plusSeconds(1));
        when(orderService.payOrder(orderId)).thenReturn(paidOrder);

        // PaymentService의 initiatePayment() Stub 처리
        Payment initiatedPayment = Payment.initiate(orderId, paymentAmount);
        Payment savedPayment = new Payment(1L, orderId, paymentAmount, PaymentStatus.PENDING, now, now);
        when(paymentService.initiatePayment(orderId, paymentAmount)).thenReturn(savedPayment);

        // PaymentService의 completePayment() Stub 처리 (PENDING -> COMPLETED)
        Payment completedPayment = new Payment(1L, orderId, paymentAmount, PaymentStatus.COMPLETED, now, now.plusSeconds(2));
        when(paymentService.completePayment(savedPayment.id())).thenReturn(completedPayment);

        // Act
        Payment result = paymentFacade.processPayment(orderId, paymentAmount, usePointAmount);

        // Assert
        assertNotNull(result, "결제 결과 Payment 객체는 null이 아니어야 합니다.");
        assertEquals(PaymentStatus.COMPLETED, result.status(), "최종 결제 상태는 COMPLETED여야 합니다.");

        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).decreaseStock(101L, 5);
        verify(userPointFacade, times(1)).usePoint(order.userId(), usePointAmount);
        verify(orderService, times(1)).payOrder(orderId);
        verify(paymentService, times(1)).initiatePayment(orderId, paymentAmount);
        verify(paymentService, times(1)).completePayment(savedPayment.id());
    }

    @Test
    void 결제시_재고부족으로_예외발생() {
        // Arrange
        Long orderId = 1L;
        int paymentAmount = 100_000;
        int usePointAmount = 10_000;
        LocalDateTime now = LocalDateTime.now();

        // Order 생성 (주문 항목: 5개 주문, 가정)
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now);
        Order order = new Order(orderId, 1L, List.of(orderItem), 25000, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // productService.decreaseStock() 호출 시, 재고 부족으로 예외를 발생하도록 설정
        when(productService.decreaseStock(101L, 5))
                .thenThrow(new IllegalStateException("상품 재고가 부족합니다. productId=101"));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, paymentAmount, usePointAmount));
        assertEquals("상품 재고가 부족합니다. productId=101", ex.getMessage());

        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).decreaseStock(101L, 5);
        // 사용자 포인트 차감, 주문 상태 및 결제 관련 메서드는 호출되지 않아야 함
        verify(userPointFacade, never()).usePoint(anyLong(), anyInt());
        verify(orderService, never()).payOrder(anyLong());
        verify(paymentService, never()).initiatePayment(anyLong(), anyInt());
    }

    /**
     * 사용자 포인트 부족 실패 테스트: userPointFacade.usePoint() 호출 시, 포인트 부족 예외 발생
     */
    @Test
    void 결제시_포인트부족으로_예외발생() {
        // Arrange
        Long orderId = 1L;
        int paymentAmount = 100_000;
        int usePointAmount = 20_000; // 요구 포인트가 높게 설정됨
        LocalDateTime now = LocalDateTime.now();

        // Order 생성 (주문 항목: 5개 주문)
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now);
        Order order = new Order(orderId, 1L, List.of(orderItem), 25000, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // productService.decreaseStock()는 정상적으로 처리 (반환값 Dummy Product)
        Product dummyProduct = new Product(101L, "Dummy Product", 5000, 45, 1L, LocalDateTime.now(), LocalDateTime.now());
        when(productService.decreaseStock(101L, 5)).thenReturn(dummyProduct);

        // userPointFacade.usePoint() 호출 시, 사용자 포인트 부족으로 예외 발생하도록 설정
        when(userPointFacade.usePoint(order.userId(), usePointAmount))
                .thenThrow(new IllegalStateException("사용자 포인트가 부족합니다."));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, paymentAmount, usePointAmount));
        assertEquals("사용자 포인트가 부족합니다.", ex.getMessage());

        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).decreaseStock(101L, 5);
        verify(userPointFacade, times(1)).usePoint(order.userId(), usePointAmount);
        // 결제 관련 메서드는 호출되지 않아야 함
        verify(orderService, never()).payOrder(anyLong());
        verify(paymentService, never()).initiatePayment(anyLong(), anyInt());
    }
}