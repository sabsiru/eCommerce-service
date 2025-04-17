package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.PopularProductSummary;
import kr.hhplus.be.server.domain.product.PopularProductSummaryRepository;
import kr.hhplus.be.server.infrastructure.order.OrderItemQueryRepository;
import kr.hhplus.be.server.infrastructure.order.PopularProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularProductSummaryBatchService {

    private final OrderItemQueryRepository orderItemQueryRepository;
    private final PopularProductSummaryRepository summaryRepository;

    @Transactional
    public void updateSummary(LocalDateTime now) {
        LocalDate targetDate = now.minusDays(1).toLocalDate();
        summaryRepository.deleteAll();

        List<PopularProductRow> rows = orderItemQueryRepository.findPopularProducts();

        List<PopularProductSummary> entities = rows.stream()
                .map(r -> new PopularProductSummary(
                        r.getProductId(),
                        r.getTotalQuantity(),
                        targetDate
                ))
                .toList();

        summaryRepository.saveAll(entities);
    }
}