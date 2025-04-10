package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.coupon.UserCouponService;
import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserPointFacade userPointFacade;
    private final PaymentService paymentService;

    private final CouponService couponService;
    private final UserCouponService userCouponService;

    /**
     * 결제 프로세스:
     * 1. 주문 조회: 주문 정보를 OrderService.getOrderOrThrow()로 조회한다.
     * 2. 상품 재고 차감: 각 주문 항목에 대해 productService.decreaseStock()를 호출하여 재고 차감 및 내부 검증을 진행한다.
     * 3. 사용자 포인트 차감: userPointFacade.usePoint()를 호출하여 포인트 차감 및 검증을 처리한다.
     * 4. 주문 상태 업데이트: OrderService.payOrder()를 호출해 주문 상태를 PAID로 전환한다.
     * 5. 결제 상태 업데이트: PaymentService를 사용하여 Payment 객체를 생성(initiating)한 후, completePayment()를 호출해
     * Payment의 상태를 COMPLETED로 전환한다.
     *
     * @param orderId       주문 ID
     * @param paymentAmount 결제 금액
     * @return 최종적으로 결제 완료된 Payment 객체
     */
    public Payment processPayment(Long orderId, int paymentAmount) {
        // 1. 주문 조회
        Order order = orderService.getOrderOrThrow(orderId);

        // 2. 상품 재고 차감: 각 주문 항목에 대해 decreaseStock() 호출
        List<OrderItem> items = order.items();
        for (Object obj : items) {
            OrderItem item = (OrderItem) obj;
            productService.decreaseStock(item.productId(), item.quantity());
        }

        int calculateDiscount = 0;
        // 쿠폰 조회시 쿠폰이 있으면 사용
        List<UserCoupon> byUserId = userCouponService.findByUserId(order.userId());
        if (!byUserId.isEmpty()) {
            //쿠폰은 조회된 첫번째 쿠폰만 사용(UI)
            calculateDiscount = calculateDiscount(byUserId.get(0).couponId(), orderId);
            userCouponService.useCoupon(byUserId.get(0).couponId());
        }

        int finalPaymentAmount = paymentAmount - calculateDiscount;
        userPointFacade.usePoint(order.userId(), finalPaymentAmount);

        // 4. 주문 상태 업데이트: 주문을 PAID 상태로 전환
        orderService.payOrder(orderId);
        Payment payment;
        // 5. 결제 상태 업데이트: Payment 초기화 후, completePayment()를 통해 Payment 상태를 COMPLETED로 전환
        if (!byUserId.isEmpty()) {
            payment = paymentService.initiatePayment(orderId, finalPaymentAmount, byUserId.get(0).couponId());
        } else {
            payment = paymentService.initiatePayment(orderId, finalPaymentAmount);
        }
        payment = paymentService.completePayment(payment.id());

        return payment;
    }

    public Payment processRefund(Long orderId) {
        // 1. 주문 조회 및 상태 변경 (환불 처리)
        Order order = orderService.getOrderOrThrow(orderId);

        // 2. 상품 재고 증가 :
        List<OrderItem> items = order.items();
        for (Object obj : items) {
            OrderItem item = (OrderItem) obj;
            productService.increaseStock(item.productId(), item.quantity());
        }

        // 3. 사용자 포인트 충전: 상태 환불
        userPointFacade.refundPoint(order.userId(), order.totalAmount(), order.id());

        // 4. 환불 상태 업데이트
        Payment refundPayment = paymentService.refundPayment(orderId);// 업데이트된 주문을 저장

        // 5. 환불된 Payment에 쿠폰 사용 내역이 있으면 쿠폰 복원 처리
        if (refundPayment.couponId() != null) {
            // userCouponService.refundCoupon()은 쿠폰의 상태를 USED에서 ISSUED로 복원하면서, Repository에 저장합니다.
            userCouponService.refundCoupon(refundPayment.couponId());
        }

        return refundPayment;
    }

    /**
     * 쿠폰을 사용하여 할인 금액을 계산합니다.
     */
    public int calculateDiscount(Long couponId, long orderId) {
        Coupon coupon = couponService.getCouponOrThrow(couponId);
        Order orderOrThrow = orderService.getOrderOrThrow(orderId);
        int totalAmount = orderOrThrow.totalAmount();
        return coupon.calculateDiscountAmount(totalAmount);
    }
}