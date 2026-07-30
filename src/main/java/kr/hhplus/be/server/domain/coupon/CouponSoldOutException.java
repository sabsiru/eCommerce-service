package kr.hhplus.be.server.domain.coupon;

public class CouponSoldOutException extends IllegalStateException {
    public CouponSoldOutException(String message) {
        super(message);
    }
}
