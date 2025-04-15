package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
        product.decreaseStock(decreaseQuantity);  // 내부 상태 변경

        return productRepository.save(product);   // 변경 감지 or 명시적 저장
    }

    // 재고 증가
    public Product increaseStock(long productId, int increaseQuantity) {
        Product product = getProductOrThrow(productId);
        product.increaseStock(increaseQuantity);

        return productRepository.save(product);
    }

    // 현재 재고 조회
    public int getStock(Long productId) {
        Product product = getProductOrThrow(productId);
        return product.getStock();  // getter 사용
    }

    public void checkStock(long productId, int requiredQuantity) {
        Product product = getProductOrThrow(productId);
        if (product.getStock() < requiredQuantity) {
            throw new IllegalStateException("상품 재고가 부족합니다.");
        }
    }
}