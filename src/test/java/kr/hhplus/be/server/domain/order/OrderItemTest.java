package kr.hhplus.be.server.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void 주문항목_정상생성_및_총금액_계산() {
        // given
        Long orderId = 100L;
        Long productId = 200L;
        int quantity = 3;
        int unitPrice = 15000;

        // when
        OrderItem item = OrderItem.create(orderId, productId, quantity, unitPrice);

        // then
        assertNull(item.id());
        assertEquals(orderId, item.orderId());
        assertEquals(productId, item.productId());
        assertEquals(quantity, item.quantity());
        assertEquals(unitPrice, item.orderPrice());
        assertEquals(quantity * unitPrice, item.totalPrice());
        assertNotNull(item.createdAt());
    }

    @Test
    void 주문항목_생성실패_orderId_null() {
        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(null, 200L, 1, 10000)
                );
        assertEquals("주문 정보가 잘 못 입력 되었습니다.", e.getMessage());
    }

    @Test
    void 주문항목_생성실패_productId_null() {
        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(100L, null, 1, 10000)
                );
        assertEquals("상품 정보가 잘 못 입력 되었습니다.", e.getMessage());
    }

    @Test
    void 주문항목_생성실패_수량_0이하() {
        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(100L, 200L, 0, 10000));
        assertEquals("수량은 0보다 커야 합니다.", e.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(100L, 200L, -5, 10000));
        assertEquals("수량은 0보다 커야 합니다.", e2.getMessage());
    }

    @Test
    void 주문항목_생성실패_주문가격_0이하() {
        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(100L, 200L, 1, 0));
        assertEquals("주문 가격은 0보다 커야 합니다.", e.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(100L, 200L, 1, -1000));
        assertEquals("주문 가격은 0보다 커야 합니다.", e2.getMessage());
    }
}