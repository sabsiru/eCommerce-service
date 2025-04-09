package kr.hhplus.be.server.domain.product;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository {

    Product save(Product product);

    // 상품 조회
    Optional<Product> findById(Long productId);

    //모든 상품 조회
    List<Product> findAll();

    List<Product> findTopSellingProducts(LocalDateTime dateTime);
}
