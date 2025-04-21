package kr.hhplus.be.server.application.payment;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentFacade {

    private final OrderService orderService;
    private final UserPointFacade userPointFacade;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final ProductService productService;

    public Payment processPayment(Long orderId, int paymentAmount) {
        Order order = orderService.getOrderOrThrow(orderId);

        List<OrderItem> items = orderService.getOrderItems(orderId);
        for (OrderItem item : items) {
            productService.decreaseStock(item.getProductId(), item.getQuantity());
        }

        int calculateDiscount = 0;
        List<UserCoupon> byUserId = couponService.findByUserId(order.getUserId());

        if (!byUserId.isEmpty()) {
            Long couponId = byUserId.get(0).getCouponId();
            calculateDiscount = calculateDiscount(couponId, orderId);
            couponService.useCoupon(couponId);
        }

        int finalPaymentAmount = paymentAmount - calculateDiscount;
        userPointFacade.usePoint(order.getUserId(), finalPaymentAmount);

        orderService.pay(orderId);

        Payment payment;
        if (!byUserId.isEmpty()) {
            payment = paymentService.initiateWithCoupon(orderId, finalPaymentAmount, byUserId.get(0).getCouponId());
        } else {
            payment = paymentService.initiateWithoutCoupon(orderId, finalPaymentAmount);
        }

        payment = paymentService.completePayment(payment.getId());
        return payment;
    }

    public Payment processRefund(Long paymentId) {
        Payment refundPayment = paymentService.refundPayment(paymentId);

        Order order = orderService.getOrderOrThrow(refundPayment.getOrderId());


        userPointFacade.refundPoint(order.getUserId(), refundPayment.getAmount(), order.getId());

        if (refundPayment.getCouponId() != null) {
            couponService.refundCoupon(refundPayment.getCouponId());
        }

        List<OrderItem> items = orderService.getOrderItems(refundPayment.getOrderId());
        for (OrderItem item : items) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }

        return refundPayment;
    }

    public int calculateDiscount(Long couponId, long orderId) {
        Coupon coupon = couponService.getCouponOrThrow(couponId);
        Order order = orderService.getOrderOrThrow(orderId);
        return coupon.calculateDiscountAmount(order.getTotalAmount());
    }
}