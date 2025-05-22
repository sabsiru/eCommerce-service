package kr.hhplus.be.server.application.payment.event;

public interface PaymentEventPort {
    void send(PaymentCompletedEvent event);

}
