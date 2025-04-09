package kr.hhplus.be.server.domain.payment;

public enum PaymentStatus {
    PENDING,    // 결제 진행 중
    COMPLETED,  // 결제 완료
    REFUND    // 환불
}