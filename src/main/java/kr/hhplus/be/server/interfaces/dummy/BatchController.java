package kr.hhplus.be.server.interfaces.dummy;

import kr.hhplus.be.server.application.product.PopularProductSummaryBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/batch")
@RequiredArgsConstructor
public class BatchController {

    private final PopularProductSummaryBatchService batchService;

//    /**
//     * 인기 상품 요약 배치를 즉시 한 번 실행합니다.
//     */
//    @PostMapping("/refresh-popular-products")
//    public ResponseEntity<Void> refreshPopularProducts() {
//        batchService.updateSummary(LocalDateTime.now());
//        return ResponseEntity.ok().build();
//    }
}
