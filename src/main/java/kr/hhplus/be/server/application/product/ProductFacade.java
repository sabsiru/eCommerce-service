package kr.hhplus.be.server.application.product;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.product.ProductReadService;
import kr.hhplus.be.server.infrastructure.product.ProductSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductFacade {

    private final PopularProductService popularProductService;
    private final ProductReadService productReadService;

    public List<ProductSummaryRow> getLatestProducts(int offset, int limit) {
        return productReadService.getLatestProducts(offset, limit);
    }

    public List<ProductSummaryRow> getLatestProductsCursor(LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return productReadService.getLatestProductsCursor(cursorCreatedAt, cursorId,limit);
    }

    public List<PopularProductInfo> getPopularProducts() {
        return popularProductService.getPopularProducts();
    }

    //조회 테이블
    public List<PopularProductInfo> getPopularProductsView() {
        return popularProductService.getPopularProductsView();
    }
}
