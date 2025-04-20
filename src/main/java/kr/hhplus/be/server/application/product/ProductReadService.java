package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.infrastructure.product.ProductQueryRepository;
import kr.hhplus.be.server.infrastructure.product.ProductSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReadService {

    private final ProductQueryRepository productQueryRepository;

    public List<ProductSummaryRow> getLatestProducts(int offset, int limit) {
        return productQueryRepository.findLatestProducts(offset, limit);
    }

    public List<ProductSummaryRow> getLatestProductsCursor(LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return productQueryRepository.findProductsByCursor(cursorCreatedAt, cursorId, limit);
    }
}