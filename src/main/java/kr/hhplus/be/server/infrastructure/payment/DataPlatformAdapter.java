package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.application.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.application.payment.event.PaymentEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataPlatformAdapter implements PaymentEventPort {
    private final DataPlatformHttpSender sendService;

    @Override
    public void send(PaymentCompletedEvent event) {
        sendService.send(event);
    }
}
