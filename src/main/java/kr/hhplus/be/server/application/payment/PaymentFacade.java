package kr.hhplus.be.server.application.payment;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentService;
import kr.hhplus.be.server.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentFacade {

    private final OrderService orderService;
    private final UserPointFacade userPointFacade;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final ProductService productService;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PRODUCT_SALES_KEY = "product:sales:daily";
    private static final long TTL_DAYS = 4;

    public Payment processPayment(Long orderId, int paymentAmount) {
        Order order = orderService.getOrderOrThrowPaid(orderId);
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

        List<OrderItem> items = orderService.getOrderItems(orderId);
        String key = PRODUCT_SALES_KEY + ":" + today;

        for (OrderItem item : items) {
            productService.decreaseStock(item.getProductId(), item.getQuantity());

            redisTemplate.opsForZSet().incrementScore(
                    key,
                    String.valueOf(item.getProductId()),
                    item.getQuantity()
            );
        }

        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);

        int calculateDiscount = 0;
        List<UserCoupon> byUserId = couponService.findByUserId(order.getUserId());

        if (!byUserId.isEmpty()) {
            Long couponId = byUserId.get(0).getCouponId();
            calculateDiscount = calculateDiscount(couponId, orderId);
            couponService.use(couponId);
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

        payment = paymentService.complete(payment.getId());
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

    public int calculateDiscount(Long couponId, long orderId) {
        Coupon coupon = couponService.getCouponOrThrow(couponId);
        Order order = orderService.getOrderOrThrowPaid(orderId);
        return coupon.calculateDiscountAmount(order.getTotalAmount());
    }
}