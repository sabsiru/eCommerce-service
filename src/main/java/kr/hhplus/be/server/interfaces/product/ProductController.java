package kr.hhplus.be.server.interfaces.product;

import kr.hhplus.be.server.application.product.PopularProductInfo;
import kr.hhplus.be.server.application.product.ProductFacade;
import kr.hhplus.be.server.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductFacade productFacade;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productFacade.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/popular")
    public List<PopularProductInfo> getPopularProducts(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate
    ) {
        return productFacade.getPopularProducts();
    }
}
