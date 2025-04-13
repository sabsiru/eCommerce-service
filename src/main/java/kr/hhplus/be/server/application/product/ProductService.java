package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    // 상품 조회: 존재하지 않으면 예외 발생
    public Product getProductOrThrow(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId=" + productId));
    }

    // 모든 상품 조회
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 재고 차감
    public Product decreaseStock(long productId, int decreaseQuantity) {
        Product product = getProductOrThrow(productId);
        Product updatedProduct = product.decreaseStock(decreaseQuantity);

        return productRepository.save(updatedProduct);
    }

    // 재고 증가
    public Product increaseStock(long productId, int increaseQuantity) {
        Product product = getProductOrThrow(productId);
        Product updatedProduct = product.increaseStock(increaseQuantity);

        return productRepository.save(updatedProduct);
    }

    public int getStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다. productId=" + productId));
        return product.stock();
    }

}
