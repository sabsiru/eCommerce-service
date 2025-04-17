package kr.hhplus.be.server.infrastructure.product;

import kr.hhplus.be.server.domain.product.Product;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductQueryRepository {
    List<ProductSummaryRow> findLatestProducts(int offset, int limit);
    List<ProductSummaryRow> findProductsByCursor(LocalDateTime cursorCreatedAt, Long cursorId, int limit);
}
