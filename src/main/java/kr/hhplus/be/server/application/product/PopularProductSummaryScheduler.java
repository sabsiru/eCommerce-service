package kr.hhplus.be.server.application.product;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PopularProductSummaryScheduler {

    private final PopularProductSummaryBatchService batchService;

    /** 매일 새벽 1시에 실행 */
    @Scheduled(cron = "0 0 1 * * *")
    public void runDaily() {
        batchService.updateSummary(LocalDateTime.now());
    }

}