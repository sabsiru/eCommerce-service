package kr.hhplus.be.server.domain.coupon.event;

public interface CouponIssuedProducer {
    void send(CouponIssuedMessage message);
}
