package kr.hhplus.be.server.domain.coupon.event;

public interface CouponIssuedMessageSender {
    void send(CouponIssuedMessage message);
}
