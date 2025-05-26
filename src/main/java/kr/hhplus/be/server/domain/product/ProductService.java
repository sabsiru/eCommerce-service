package kr.hhplus.be.server.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product getProductForUpdate(long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId=" + productId));
    }

    public Product getProduct(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId=" + productId));
    }

    public Page<Product> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional
    public Product decreaseStock(long productId, int decreaseQuantity) {
        Product product = getProductForUpdate(productId);
        product.decreaseStock(decreaseQuantity);

        return productRepository.save(product);
    }

    @Transactional
    public Product increaseStock(long productId, int increaseQuantity) {
        Product product = getProductForUpdate(productId);
        product.increaseStock(increaseQuantity);

        return productRepository.save(product);
    }

    public int checkStock(Long productId) {
        Product product = getProduct(productId);
        return product.getStock();
    }

    public void checkStock(long productId, int requiredQuantity) {
        Product product = getProduct(productId);
        if (product.getStock() < requiredQuantity) {
            throw new IllegalStateException("상품 재고가 부족합니다.");
        }
    }
}