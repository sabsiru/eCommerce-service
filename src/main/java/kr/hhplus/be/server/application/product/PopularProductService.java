package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.PopularProductSummary;
import kr.hhplus.be.server.domain.product.PopularProductSummaryRepository;
import kr.hhplus.be.server.infrastructure.order.OrderItemQueryRepository;
import kr.hhplus.be.server.infrastructure.order.PopularProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
public class PopularProductService {

    private final OrderItemQueryRepository orderItemQueryRepository;
    private final PopularProductSummaryRepository popularProductSummaryRepository;

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PRODUCT_SALES_KEY = "product:sales:daily";
    private static final int DAYS_TO_KEEP = 3;
    private static final int TOP_N = 5;

    public List<PopularProductInfo> getPopularProductsRedis() {
        List<String> keys = IntStream.range(0, DAYS_TO_KEEP)
                .mapToObj(i -> PRODUCT_SALES_KEY + ":" +
                        LocalDate.now().minusDays(i).format(DateTimeFormatter.ISO_DATE))
                .toList();

        String unionKey = PRODUCT_SALES_KEY + ":temp";

        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            redisTemplate.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), unionKey);

            Set<ZSetOperations.TypedTuple<String>> topProducts =
                    redisTemplate.opsForZSet().reverseRangeWithScores(unionKey, 0, TOP_N - 1);

            return Optional.ofNullable(topProducts)
                    .orElse(Collections.emptySet())
                    .stream()
                    .map(tuple -> new PopularProductInfo(
                            Long.parseLong(tuple.getValue()),
                            tuple.getScore().intValue()))
                    .toList();
        } finally {
            redisTemplate.delete(unionKey);
        }
    }

    @Cacheable(value = "popularProducts", key = "'top5'", unless = "#result == null || #result.isEmpty()")
    public List<PopularProductInfo> getPopularProducts() {
        return loadAndCachePopularProducts();
    }
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