package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.coupon.UserCouponService;
import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponStatus;
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
    private UserCouponService userCouponService;

    @Mock
    private CouponService couponService;

    @Test
    void 결제_정상_작동_쿠폰사용_없음() {
        // Arrange
        Long orderId = 1L;
        int paymentAmount = 100_000;

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
        when(userPointFacade.usePoint(order.userId(), paymentAmount)).thenReturn(updatedUser);

        // orderService.payOrder()를 통해 주문 상태 PAID로 변경된 Order 반환
        Order paidOrder = new Order(orderId, order.userId(), order.items(), order.totalAmount(), OrderStatus.PAID, now, now.plusSeconds(1));
        when(orderService.payOrder(orderId)).thenReturn(paidOrder);

        // 쿠폰 조회: 사용 가능한 쿠폰이 없으므로 빈 리스트 반환
        when(userCouponService.findByUserId(order.userId())).thenReturn(Collections.emptyList());

        // PaymentService의 initiatePayment() Stub 처리
        Payment savedPayment = new Payment(1L, orderId, paymentAmount, PaymentStatus.PENDING, now, now,null);
        when(paymentService.initiatePayment(orderId, paymentAmount)).thenReturn(savedPayment);

        // PaymentService의 completePayment() Stub 처리 (PENDING -> COMPLETED)
        Payment completedPayment = new Payment(1L, orderId, paymentAmount, PaymentStatus.COMPLETED, now, now.plusSeconds(2),null);
        when(paymentService.completePayment(savedPayment.id())).thenReturn(completedPayment);

        // Act
        Payment result = paymentFacade.processPayment(orderId, paymentAmount);

        // Assert
        assertNotNull(result, "결제 결과 Payment 객체는 null이 아니어야 합니다.");
        assertEquals(PaymentStatus.COMPLETED, result.status(), "최종 결제 상태는 COMPLETED여야 합니다.");
        assertNull(result.couponId(), "쿠폰을 사용하지 않은 경우 couponId는 null이어야 합니다.");

        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).decreaseStock(101L, 5);
        verify(userPointFacade, times(1)).usePoint(order.userId(), paymentAmount);
        verify(orderService, times(1)).payOrder(orderId);
        verify(paymentService, times(1)).initiatePayment(orderId, paymentAmount);
        verify(paymentService, times(1)).completePayment(savedPayment.id());
    }

    @Test
    void 결제_정상_작동_쿠폰사용_및할인검증_테스트() {
        // Arrange
        Long orderId = 1L;
        // 결제 금액(주문 총액)이 25,000원인 상황
        int paymentAmount = 25_000;
        // 쿠폰 할인율 30%이면 25,000 * 0.3 = 7,500원이지만 최대 할인액 5,000원 적용
        int expectedDiscount = 5_000;
        int finalPaymentAmount = paymentAmount - expectedDiscount; // 20,000원
        Long couponId = 500L;
        LocalDateTime now = LocalDateTime.now();

        // 1. 주문 조회: 주문 항목에 의한 총액 25,000원
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now); // 5 * 5000 = 25,000
        Order order = new Order(orderId, 1L, List.of(orderItem), 25000, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // 2. 재고 차감: productService.decreaseStock 호출 시 Product 반환
        Product dummyProduct = new Product(101L, "Dummy Product", 5000, 45, 1L, now, now);
        when(productService.decreaseStock(101L, 5)).thenReturn(dummyProduct);

        // 3. 쿠폰 조회: 사용 가능한 쿠폰이 있으므로 리스트 반환
        UserCoupon userCoupon = new UserCoupon(1L, order.userId(), couponId, null, now, now);
        when(userCouponService.findByUserId(order.userId())).thenReturn(List.of(userCoupon));

        // 4. CouponService Stub: couponService.getCouponOrThrow(couponId)에서 Coupon 객체 반환
        Coupon dummyCoupon = mock(Coupon.class);
        when(couponService.getCouponOrThrow(couponId)).thenReturn(dummyCoupon);
        // Coupon 객체의 calculateDiscountAmount() 호출 시, 결제금액 25,000원을 인자로 받아 최대 할인 5,000원을 반환하도록 설정
        when(dummyCoupon.calculateDiscountAmount(paymentAmount)).thenReturn(expectedDiscount);

        // 5. 쿠폰 사용 처리: 사용된 쿠폰 객체 반환
        UserCoupon usedCoupon = new UserCoupon(1L, order.userId(), couponId, null, now, now);
        when(userCouponService.useCoupon(couponId)).thenReturn(usedCoupon);

        // 6. 포인트 차감: 최종 결제 금액(20,000원) 적용하여 User 업데이트 Stub 처리
        User updatedUser = new User(order.userId(), "TestUser", 40000, now, now);
        when(userPointFacade.usePoint(order.userId(), finalPaymentAmount)).thenReturn(updatedUser);

        // 7. 주문 상태 업데이트: 주문 상태 PAID로 변경된 주문 반환
        Order paidOrder = new Order(orderId, order.userId(), order.items(), order.totalAmount(), OrderStatus.PAID, now, now.plusSeconds(1));
        when(orderService.payOrder(orderId)).thenReturn(paidOrder);

        // 8. 결제 초기화: 쿠폰 사용이 적용되므로 couponId 포함한 Payment 생성
        Payment savedPayment = new Payment(1L, orderId, finalPaymentAmount, PaymentStatus.PENDING, now, now, couponId);
        when(paymentService.initiatePayment(orderId, finalPaymentAmount, couponId)).thenReturn(savedPayment);

        // 9. 결제 완료: Payment 상태가 COMPLETED로 전환된 Payment 반환
        Payment completedPayment = new Payment(1L, orderId, finalPaymentAmount, PaymentStatus.COMPLETED, now, now.plusSeconds(2), couponId);
        when(paymentService.completePayment(savedPayment.id())).thenReturn(completedPayment);

        // Act
        Payment result = paymentFacade.processPayment(orderId, paymentAmount);

        // Assert
        assertNotNull(result, "결제 결과 Payment 객체는 null이 아니어야 합니다.");
        assertEquals(PaymentStatus.COMPLETED, result.status(), "최종 결제 상태는 COMPLETED여야 합니다.");
        assertEquals(couponId, result.couponId(), "쿠폰 사용 시 couponId가 반영되어야 합니다.");
        assertEquals(finalPaymentAmount, result.amount(), "할인 적용 최종 결제 금액이 올바르게 계산되어야 합니다.");

        verify(orderService, times(2)).getOrderOrThrow(orderId);
        verify(productService, times(1)).decreaseStock(101L, 5);
        verify(userCouponService, times(1)).findByUserId(order.userId());
        verify(couponService, times(1)).getCouponOrThrow(couponId);
        verify(dummyCoupon, times(1)).calculateDiscountAmount(paymentAmount);
        verify(userCouponService, times(1)).useCoupon(couponId);
        verify(userPointFacade, times(1)).usePoint(order.userId(), finalPaymentAmount);
        verify(orderService, times(1)).payOrder(orderId);
        verify(paymentService, times(1)).initiatePayment(orderId, finalPaymentAmount, couponId);
        verify(paymentService, times(1)).completePayment(savedPayment.id());
    }

    @Test
    void 결제시_재고부족으로_예외발생() {
        // Arrange
        Long orderId = 1L;
        int paymentAmount = 100_000;
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
                () -> paymentFacade.processPayment(orderId, paymentAmount));
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
        LocalDateTime now = LocalDateTime.now();

        // Order 생성 (주문 항목: 5개 주문)
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now);
        Order order = new Order(orderId, 1L, List.of(orderItem), 25000, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // productService.decreaseStock()는 정상적으로 처리 (반환값 Dummy Product)
        Product dummyProduct = new Product(101L, "Dummy Product", 5000, 45, 1L, LocalDateTime.now(), LocalDateTime.now());
        when(productService.decreaseStock(101L, 5)).thenReturn(dummyProduct);

        // userPointFacade.usePoint() 호출 시, 사용자 포인트 부족으로 예외 발생하도록 설정
        when(userPointFacade.usePoint(order.userId(), paymentAmount))
                .thenThrow(new IllegalStateException("사용자 포인트가 부족합니다."));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentFacade.processPayment(orderId, paymentAmount));
        assertEquals("사용자 포인트가 부족합니다.", ex.getMessage());

        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).decreaseStock(101L, 5);
        verify(userPointFacade, times(1)).usePoint(order.userId(), paymentAmount);
        // 결제 관련 메서드는 호출되지 않아야 함
        verify(orderService, never()).payOrder(anyLong());
        verify(paymentService, never()).initiatePayment(anyLong(), anyInt());
    }

    @Test
    void 환불_정상처리() {
        // 준비 (Arrange)
        Long orderId = 1L;
        int refundAmount = 25000; // 주문의 총액이 25000이라고 가정
        LocalDateTime now = LocalDateTime.now();

        // 1. 주문 조회: 주문에 포함된 주문 항목이 1건 있다고 가정 (예: 5개 주문, 5000 단가 → 총액 25000)
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now);
        Order order = new Order(orderId, 1L, List.of(orderItem), 25000, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // 2. 상품 재고 증가: 각 주문 항목에 대해 increaseStock()를 호출하면, 해당 Product 객체를 반환하도록 Stub 처리
        // 각 주문 항목별로 주문 수량만큼 재고가 복원됨. (여기서는 101번 상품, 주문 수량 5)
        Product dummyProduct = new Product(101L, "상품 A", 5000, 55, 1L, now, now);
        when(productService.increaseStock(eq(101L), eq(orderItem.quantity()))).thenReturn(dummyProduct);

        // 3. 사용자 포인트 환불(복원): userPointFacade.refundPoint() 호출 시, 복원된 User 객체를 반환하도록 Stub 처리
        // 예를 들어, 환불 시 주문의 총액(25000)을 다시 충전한다고 가정
        User refundedUser = new User(order.userId(), "테스터", 25000, now.minusDays(1), now);
        when(userPointFacade.refundPoint(1L, 25000, 1L))
                .thenReturn(refundedUser);

        // 4. 환불 상태 업데이트: PaymentService.refundPayment() 호출 시, 환불 처리된 Payment 객체 반환
        Payment dummyPayment = new Payment(1L, 1L, 25000, PaymentStatus.REFUND, now, now.plusSeconds(2),null); // Payment.refund()는 환불 Payment 객체를 생성하는 정적 팩토리 메서드라 가정
        when(paymentService.refundPayment(orderId)).thenReturn(dummyPayment);

        // 실행 (Act)
        Payment result = paymentFacade.processRefund(orderId);

        // 검증 (Assert)
        assertEquals(dummyPayment, result);

        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).increaseStock(eq(101L), eq(orderItem.quantity()));
        verify(userPointFacade, times(1)).refundPoint(eq(order.userId()), eq(order.totalAmount()), eq(order.id()));
        verify(paymentService, times(1)).refundPayment(orderId);
    }

    @Test
    void 환불_로직_쿠폰없을때_테스트() {
        // Arrange
        Long orderId = 1L;
        int totalAmount = 25000;  // 주문 총액(쿠폰 할인 없이)
        LocalDateTime now = LocalDateTime.now();

        // 주문 항목: 상품 id 101, 수량 5, 단가 5000 → 총액 25000
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now);
        Order order = new Order(orderId, 1L, List.of(orderItem), totalAmount, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // 재고 복원: productService.increaseStock() 호출 (반환값 필요없음)
        // 포인트 환급: userPointFacade.refundPoint() stub 처리 (업데이트된 User 반환)
        User dummyUser = new User(order.userId(), "TestUser", 30000, now, now);
        when(userPointFacade.refundPoint(order.userId(), totalAmount, orderId)).thenReturn(dummyUser);

        // Payment 환불 처리: 쿠폰이 없으므로 refundPayment()에서 couponId == null
        Payment refundPayment = new Payment(1L, orderId, totalAmount, PaymentStatus.REFUND, now, now.plusSeconds(1), null);
        when(paymentService.refundPayment(orderId)).thenReturn(refundPayment);

        // Act
        Payment result = paymentFacade.processRefund(orderId);

        // Assert
        assertNotNull(result, "환불 결과 Payment 객체는 null이 아니어야 합니다.");
        assertNull(result.couponId(), "쿠폰이 없는 경우 couponId는 null이어야 합니다.");
        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).increaseStock(101L, 5);
        verify(userPointFacade, times(1)).refundPoint(order.userId(), totalAmount, orderId);
        // 쿠폰 사용 없으므로 couponService.refundCoupon() 호출되지 않아야 함
        verify(userCouponService, never()).refundCoupon(any());
        verify(paymentService, times(1)).refundPayment(orderId);
    }

    // 환불 시 쿠폰 사용 내역이 있는 경우
    @Test
    void 환불_로직_쿠폰있을때_테스트() {
        // Arrange
        Long orderId = 1L;
        int totalAmount = 25000;
        LocalDateTime now = LocalDateTime.now();
        Long couponId = 500L;

        // 주문 항목: 상품 id 101, 수량 5, 단가 5000 → 총액 25000
        OrderItem orderItem = new OrderItem(1L, orderId, 101L, 5, 5000, now);
        Order order = new Order(orderId, 1L, List.of(orderItem), totalAmount, OrderStatus.PENDING, now, now);
        when(orderService.getOrderOrThrow(orderId)).thenReturn(order);

        // 포인트 환급
        User dummyUser = new User(order.userId(), "TestUser", 30000, now, now);
        when(userPointFacade.refundPoint(order.userId(), totalAmount, orderId)).thenReturn(dummyUser);

        // Payment refund 처리: 환불 Payment에 couponId가 기록됨
        Payment refundPayment = new Payment(1L, orderId, totalAmount, PaymentStatus.REFUND, now, now.plusSeconds(1), couponId);
        when(paymentService.refundPayment(orderId)).thenReturn(refundPayment);

        // 쿠폰 복원 처리: couponService.refundCoupon()가 호출되면 ISSUED 상태의 Coupon 반환
        UserCoupon refundedCoupon = new UserCoupon(
                couponId,
                1L,
                couponId,
                UserCouponStatus.ISSUED,
                now.plusDays(1),
                null
        );
        when(userCouponService.refundCoupon(couponId)).thenReturn(refundedCoupon);

        // Act
        Payment result = paymentFacade.processRefund(orderId);

        // Assert
        assertNotNull(result, "환불 결과 Payment 객체는 null이 아니어야 합니다.");
        assertNotNull(result.couponId(), "쿠폰 사용 내역이 있으면 couponId가 있어야 합니다.");
        assertEquals(couponId, result.couponId(), "환불 Payment의 couponId가 올바르게 반영되어야 합니다.");
        verify(orderService, times(1)).getOrderOrThrow(orderId);
        verify(productService, times(1)).increaseStock(101L, 5);
        verify(userPointFacade, times(1)).refundPoint(order.userId(), totalAmount, orderId);
        verify(userCouponService, times(1)).refundCoupon(couponId);
        verify(paymentService, times(1)).refundPayment(orderId);
    }

}