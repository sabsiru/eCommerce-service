package kr.hhplus.be.server.domain.payment.event;

public interface PaymentEventPort {
    void send(PaymentCompletedEvent event);

}
