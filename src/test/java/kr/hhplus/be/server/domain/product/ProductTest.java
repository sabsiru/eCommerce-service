package kr.hhplus.be.server.domain.product;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void 재고_조회_정상_테스트() {
        // given
        String name = "Test Product";
        int price = 10_000;
        int expectedStock = 50;
        long categoryId = 1L;

        Product product = new Product(name, price, expectedStock, categoryId);

        // when
        int actualStock = product.getStock();

        // then
        assertEquals(expectedStock, actualStock, "초기 재고가 올바르게 설정되어야 합니다.");
    }

    @Test
    void 재고_차감_정상_테스트() {
        // given
        Product product = new Product("Test Product", 10_000, 100, 1L);
        int purchaseQuantity = 10;

        // when
        product.decreaseStock(purchaseQuantity);

        // then
        assertEquals(90, product.getStock());
    }

    @Test
    void 재고_증가_정상_테스트() {
        // given
        Product product = new Product("Test Product", 10_000, 50, 1L);
        int restockQuantity = 20;

        // when
        product.increaseStock(restockQuantity);

        // then
        assertEquals(70, product.getStock());
    }

    @Test
    void 재고_차감_재고_부족_예외_테스트() {
        // given
        Product product = new Product("Test Product", 10_000, 5, 1L);
        int purchaseQuantity = 10;

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> product.decreaseStock(purchaseQuantity));
        assertEquals("재고가 부족합니다.", exception.getMessage());
    }

    @Test
    void 재고_차감_구매_수량_0이하_예외_테스트() {
        // given
        Product product = new Product("Test Product", 10_000, 100, 1L);
        int purchaseQuantity = 0;

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> product.decreaseStock(purchaseQuantity));
        assertEquals("구매 수량은 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    void 재입고_0이하_예외_테스트() {
        // given
        Product product = new Product("Test Product", 10_000, 100, 1L);
        int restockQuantity = 0;

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> product.increaseStock(restockQuantity));
        assertEquals("재입고 수량은 0보다 커야 합니다.", exception.getMessage());
    }
}