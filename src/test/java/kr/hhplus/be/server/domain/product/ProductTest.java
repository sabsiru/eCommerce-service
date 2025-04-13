package kr.hhplus.be.server.domain.product;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void 재고_조회_정상_테스트() {
        // given
        Long productId = 1L;
        String name = "Test Product";
        int price = 10_000;
        int expectedStock = 50;
        long categoryId = 1L;
        // 생성 시각
        LocalDateTime now = LocalDateTime.now();

        Product product = new Product(productId, name, price, expectedStock, categoryId, now, now);

        // when
        int actualStock = product.stock(); // 또는 getStock()을 사용한다면 그 메서드를 호출

        // then
        assertEquals(expectedStock, actualStock, "초기 재고가 올바르게 설정되어야 합니다.");
    }
    @Test
    void 재고_차감_정상_테스트() {
        // given
        Long productId = 1L;
        String name = "Test Product";
        int price = 10_000;
        int stock = 100; // 초기 재고
        long categoryId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(productId, name, price, stock, categoryId, now, now);

        int purchaseQuantity = 10; // 구매 수량

        // when
        Product updatedProduct = product.decreaseStock(purchaseQuantity);

        // then: 재고가 올바르게 차감되어야 함
        assertEquals(stock - purchaseQuantity, updatedProduct.stock());
    }

    @Test
    void 재고_증가_정상_테스트() {
        // given
        Long productId = 1L;
        String name = "Test Product";
        int price = 10_000;
        int stock = 50; // 초기 재고
        long categoryId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(productId, name, price, stock, categoryId, now, now);

        int restockQuantity = 20; // 재입고 수량

        // when
        Product updatedProduct = product.increaseStock(restockQuantity);

        // then: 재고가 올바르게 증가되어야 함
        assertEquals(stock + restockQuantity, updatedProduct.stock());
    }

    @Test
    void 재고_차감_재고_부족_예외_테스트() {
        // given
        Long productId = 1L;
        String name = "Test Product";
        int price = 10_000;
        int stock = 5; // 재고 부족 상황
        long categoryId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(productId, name, price, stock, categoryId, now, now);

        int purchaseQuantity = 10; // 구매 수량이 재고보다 많음

        // when & then: 재고 부족 예외가 발생해야 함
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> product.decreaseStock(purchaseQuantity));
        assertEquals("재고가 부족합니다.", exception.getMessage());
    }

    @Test
    void 재고_차감_구매_수량_0이하_예외_테스트() {
        // given
        Long productId = 1L;
        String name = "Test Product";
        int price = 10_000;
        int stock = 100;
        long categoryId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(productId, name, price, stock, categoryId, now, now);

        int purchaseQuantity = 0; // 잘못된 구매 수량

        // when & then: 구매 수량이 0 이하인 경우 예외 발생
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> product.decreaseStock(purchaseQuantity));
        assertEquals("구매 수량은 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    void 재입고_0이하_예외_테스트() {
        // given
        Long productId = 1L;
        String name = "Test Product";
        int price = 10_000;
        int stock = 100;
        long categoryId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(productId, name, price, stock, categoryId, now, now);

        int restockQuantity = 0; // 잘못된 재입고 수량

        // when & then: 재입고 수량이 0 이하인 경우 예외 발생
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> product.increaseStock(restockQuantity));
        assertEquals("재입고 수량은 0보다 커야 합니다.", exception.getMessage());
    }
}