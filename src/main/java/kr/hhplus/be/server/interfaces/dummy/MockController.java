package kr.hhplus.be.server.interfaces.dummy;

import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock")
@Slf4j
public class MockController {

    @PostMapping("/payments")
    public ResponseEntity<Void> receivePaymentData(@RequestBody PaymentCompletedEvent event) {
        log.info("결제 데이터 수신 완료: {}", event);
        return ResponseEntity.ok().build();
    }
}
