package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDb() {
        productRepository.deleteAll();
    }

    @Test
    void 전체_상품_조회() {
        // given
        productRepository.save(new Product("상품1", 1000, 10, 1L));
        productRepository.save(new Product("상품2", 2000, 20, 1L));

        // when
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> products = productService.getProducts(pageable);

        // then
        assertThat(products).hasSize(2);
    }

    @Test
    void 상품_단건_조회_성공() {
        Product product = productRepository.save(new Product("상품", 5000, 15, 1L));

        Product found = productService.getProductForUpdate(product.getId());

        assertThat(found.getName()).isEqualTo("상품");
        assertThat(found.getPrice()).isEqualTo(5000);
    }

    @Test
    void 상품_단건_조회_실패() {
        assertThatThrownBy(() -> productService.getProductForUpdate(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productId");
    }

    @Test
    void 재고_차감_정상() {
        Product product = productRepository.save(new Product("상품", 3000, 10, 1L));

        productService.decreaseStock(product.getId(), 4);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(6);
    }

    @Test
    void 재고_부족_예외() {
        Product product = productRepository.save(new Product("상품", 3000, 2, 1L));

        assertThatThrownBy(() -> productService.decreaseStock(product.getId(), 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고가 부족합니다.");
    }

    @Test
    void 재고_증가_정상() {
        Product product = productRepository.save(new Product("상품", 1000, 5, 1L));

        productService.increaseStock(product.getId(), 7);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(12);
    }

    @Test
    void 재고_확인_충분하면_정상() {
        Product product = productRepository.save(new Product("상품", 1000, 10, 1L));

        productService.checkStock(product.getId(), 5); // 예외 없음
    }

    @Test
    void 재고_확인_부족하면_예외() {
        Product product = productRepository.save(new Product("상품", 1000, 3, 1L));

        assertThatThrownBy(() -> productService.checkStock(product.getId(), 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("상품 재고가 부족합니다.");
    }
}
