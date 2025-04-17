package kr.hhplus.be.server.application.product;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.infrastructure.product.ProductSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductFacade {

    private final ProductService productService;
    private final PopularProductService popularProductService;
    private final ProductReadService productReadService;

    public Page<Product> getProducts(Pageable pageable) {
        return productService.getProducts(pageable);
    }

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
