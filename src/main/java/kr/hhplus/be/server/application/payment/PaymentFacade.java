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
        Long couponId = couponService.getAvailableCouponId(order.getUserId());

        Payment payment = calculateDiscountAndCreatePayment(order.getUserId(), orderId, couponId, order.getTotalAmount(), paymentAmount);

        order = orderService.pay(orderId);
        paymentCompletedProducer.send(payment, order);

        return payment;
    }

    public Payment processRefund(Long paymentId) {
        Payment refundPayment = paymentService.refund(paymentId);

        Order order = orderService.getOrderOrThrowCancel(refundPayment.getOrderId());

        userPointFacade.refundPoint(order.getUserId(), refundPayment.getAmount(), order.getId());

        if (refundPayment.getCouponId() != null) {
            couponService.refund(refundPayment.getCouponId());
        }

        List<OrderItem> items = orderService.getOrderItems(refundPayment.getOrderId());
        for (OrderItem item : items) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }

        return refundPayment;
    }

    private Payment calculateDiscountAndCreatePayment(Long userId, Long orderId, Long couponId, int totalAmount, int paymentAmount) {
        int discountAmount = 0;
        if (couponId != null) {
            couponEventPublisher.publishCouponValidate(new CouponValidateEvent(userId, orderId, couponId));
            discountAmount = couponService.calculateDiscountAmount(couponId, totalAmount);
            pointEventPublisher.publishPointUsed(new PointUseEvent(userId, totalAmount - discountAmount));
        } else {
            pointEventPublisher.publishPointUsed(new PointUseEvent(userId, totalAmount));
        }

        return paymentService.create(orderId, paymentAmount - discountAmount, couponId);
    }
}