package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.application.redis.DistributedLock;
import kr.hhplus.be.server.domain.product.PopularProductSummary;
import kr.hhplus.be.server.domain.product.PopularProductSummaryRepository;
import kr.hhplus.be.server.infrastructure.order.OrderItemQueryRepository;
import kr.hhplus.be.server.infrastructure.order.PopularProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class PopularProductService {

    private final OrderItemQueryRepository orderItemQueryRepository;
    private final PopularProductSummaryRepository popularProductSummaryRepository;

    @Cacheable(value = "popularProducts", key = "'top5'", unless = "#result == null || #result.isEmpty()")
    public List<PopularProductInfo> getPopularProducts() {
        return loadAndCachePopularProducts();
    }

    @DistributedLock(
            key = "'popularProducts:top5'",
            waitTime = 1,
            leaseTime = 2,
            timeUnit = TimeUnit.SECONDS
    )
    public List<PopularProductInfo> loadAndCachePopularProducts() {
        List<PopularProductRow> rows = orderItemQueryRepository.findPopularProducts();
        return rows.stream()
                .map(row -> new PopularProductInfo(row.getProductId(), row.getTotalQuantity()))
                .toList();
    }

    public List<PopularProductInfo> getPopularProductsView() {
        List<PopularProductSummary> summaries = popularProductSummaryRepository.findAll();
        return summaries.stream()
                .map(e -> new PopularProductInfo(e.getProductId(), e.getTotalQuantity()))
                .toList();
    }
}