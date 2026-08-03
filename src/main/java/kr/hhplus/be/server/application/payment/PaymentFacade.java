package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.event.CouponEventPublisher;
import kr.hhplus.be.server.domain.coupon.event.CouponValidateEvent;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentService;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedProducer;
import kr.hhplus.be.server.domain.point.event.PointEventPublisher;
import kr.hhplus.be.server.domain.point.event.PointUseEvent;
import kr.hhplus.be.server.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final OrderService orderService;
    private final UserPointFacade userPointFacade;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final ProductService productService;
    private final CouponEventPublisher couponEventPublisher;
    private final PointEventPublisher pointEventPublisher;
    private final PaymentCompletedProducer paymentCompletedProducer;

    @Transactional
    public Payment processPayment(Long orderId, int paymentAmount) {
        Order order = orderService.getOrderOrThrowPaid(orderId);

        // paymentAmount는 클라이언트가 주문 금액에 동의했다는 확인 값일 뿐, 실제 결제/환불
        // 금액 계산의 근거로 쓰지 않는다. 서버가 계산한 주문 총액과 다르면 조작된 값으로 보고 거부한다.
        if (paymentAmount != order.getTotalAmount()) {
            throw new IllegalArgumentException(
                    "결제 금액이 주문 금액과 일치하지 않습니다. paymentAmount=" + paymentAmount
                            + ", orderTotalAmount=" + order.getTotalAmount());
        }

        Long couponId = couponService.getAvailableCouponId(order.getUserId());

        Payment payment = calculateDiscountAndCreatePayment(order.getUserId(), orderId, couponId, order.getTotalAmount());

        order = orderService.pay(orderId);

        List<OrderItem> items = orderService.getOrderItems(orderId);
        for (OrderItem item : items) {
            productService.decreaseStock(item.getProductId(), item.getQuantity());
        }

        paymentCompletedProducer.send(payment, order);

        return payment;
    }

    @Transactional
    public Payment processRefund(Long paymentId) {
        Payment refundPayment = paymentService.refund(paymentId);

        Order order = orderService.getOrderOrThrowCancel(refundPayment.getOrderId());

        userPointFacade.refundPoint(order.getUserId(), refundPayment.getAmount(), order.getId());

        if (refundPayment.getCouponId() != null) {
            couponService.refundByCoupon(order.getUserId(), refundPayment.getCouponId());
        }

        List<OrderItem> items = orderService.getOrderItems(refundPayment.getOrderId());
        for (OrderItem item : items) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }

        return refundPayment;
    }

    private Payment calculateDiscountAndCreatePayment(Long userId, Long orderId, Long couponId, int totalAmount) {
        int discountAmount = 0;
        if (couponId != null) {
            couponEventPublisher.publishCouponValidate(new CouponValidateEvent(userId, orderId, couponId));
            discountAmount = couponService.calculateDiscountAmount(couponId, totalAmount);
            pointEventPublisher.publishPointUsed(new PointUseEvent(userId, totalAmount - discountAmount));
        } else {
            pointEventPublisher.publishPointUsed(new PointUseEvent(userId, totalAmount));
        }

        return paymentService.create(orderId, totalAmount - discountAmount, couponId);
    }
}