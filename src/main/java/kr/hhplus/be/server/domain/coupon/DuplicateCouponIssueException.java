package kr.hhplus.be.server.domain.coupon;

public class DuplicateCouponIssueException extends IllegalStateException {
    public DuplicateCouponIssueException(String message) {
        super(message);
    }
}
