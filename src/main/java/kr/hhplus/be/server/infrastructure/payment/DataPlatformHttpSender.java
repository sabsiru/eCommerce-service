package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.application.payment.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataPlatformHttpSender {
    private static final String DATA_PLATFORM_URL = "http://localhost:8080/mock/payments";
    private final RestTemplate restTemplate;

    public void send(PaymentCompletedEvent event) {
        try {
            restTemplate.postForEntity(DATA_PLATFORM_URL, event, Void.class);
            log.info("데이터 플랫폼 전송 성공", event);
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: {}", e.getMessage());
        }
    }
}