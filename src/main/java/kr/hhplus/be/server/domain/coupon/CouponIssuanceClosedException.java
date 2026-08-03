package kr.hhplus.be.server.domain.coupon;

public class CouponIssuanceClosedException extends IllegalStateException {
    public CouponIssuanceClosedException(String message) {
        super(message);
    }
}
