package kr.hhplus.be.server.application.product;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductFacade {

    private final ProductService productService;
    private final PopularProductService popularProductService;

    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    public List<PopularProductInfo> getPopularProducts() {
        return popularProductService.getPopularProducts();
    }
}
