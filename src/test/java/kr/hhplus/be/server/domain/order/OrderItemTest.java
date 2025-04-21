package kr.hhplus.be.server.domain.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void 정상_생성_및_총가격_계산() {
        Order dummyOrder = new Order(1L);
        OrderItem item = OrderItem.of(dummyOrder, 200L, 3, 15000);

        assertEquals(dummyOrder, item.getOrder());
        assertEquals(200L, item.getProductId());
        assertEquals(3, item.getQuantity());
        assertEquals(15000, item.getOrderPrice());
        assertEquals(3 * 15000, item.totalPrice());
    }

    @Test
    void 주문_정보_null이면_예외() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                OrderItem.of(null, 200L, 1, 10000)
        );
        assertEquals("주문 정보가 잘 못 입력 되었습니다.", e.getMessage());
    }

    @Test
    void 상품정보_null이면_예외() {
        Order dummyOrder = new Order(1L);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                OrderItem.of(dummyOrder, null, 1, 10000)
        );
        assertEquals("상품 정보가 잘 못 입력 되었습니다.", e.getMessage());
    }

    @Test
    void 수량_0이하_예외() {
        Order dummyOrder = new Order(1L);
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                OrderItem.of(dummyOrder, 200L, 0, 10000)
        );
        assertEquals("수량은 0보다 커야 합니다.", e1.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                OrderItem.of(dummyOrder, 200L, -5, 10000)
        );
        assertEquals("수량은 0보다 커야 합니다.", e2.getMessage());
    }

    @Test
    void 가격_0이하_예외() {
        Order dummyOrder = new Order(1L);
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                OrderItem.of(dummyOrder, 200L, 1, 0)
        );
        assertEquals("주문 가격은 0보다 커야 합니다.", e1.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                OrderItem.of(dummyOrder, 200L, 1, -1000)
        );
        assertEquals("주문 가격은 0보다 커야 합니다.", e2.getMessage());
    }
}