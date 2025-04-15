package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.application.order.OrderItemCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void 주문항목_정상생성_및_총금액_계산() {
        // given
        Long productId = 200L;
        int quantity = 3;
        int unitPrice = 15000;

        // 주문 생성
        OrderItemCommand itemCommand = new OrderItemCommand(productId, quantity, unitPrice);
        Order order = Order.create(1L, List.of(itemCommand));

        OrderItem item = order.getItems().get(0); // 실제 포함된 OrderItem

        // then
        assertEquals(order, item.getOrder());
        assertEquals(productId, item.getProductId());
        assertEquals(quantity, item.getQuantity());
        assertEquals(unitPrice, item.getOrderPrice());
        assertEquals(quantity * unitPrice, item.totalPrice());
        assertNotNull(item.getCreatedAt());
    }



    @Test
    void 주문항목_생성실패_order_null() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(null, 200L, 1, 10000));
        assertEquals("주문 정보가 잘 못 입력 되었습니다.", e.getMessage());
    }

    @Test
    void 주문항목_생성실패_productId_null() {

        OrderItemCommand itemCommand = new OrderItemCommand(1L, 2, 10000);
        Order order = Order.create(1L, List.of(itemCommand));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(order, null, 1, 10000));
        assertEquals("상품 정보가 잘 못 입력 되었습니다.", e.getMessage());
    }

    @Test
    void 주문항목_생성실패_수량_0이하() {
        OrderItemCommand itemCommand = new OrderItemCommand(200L, 2, 10000);
        Order order = Order.create(1L, List.of(itemCommand));

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(order, 200L, 0, 10000));
        assertEquals("수량은 0보다 커야 합니다.", e1.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(order, 200L, -5, 10000));
        assertEquals("수량은 0보다 커야 합니다.", e2.getMessage());
    }

    @Test
    void 주문항목_생성실패_주문가격_0이하() {
        OrderItemCommand itemCommand = new OrderItemCommand(200L, 2, 10000);
        Order order = Order.create(1L, List.of(itemCommand));

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(order, 200L, 1, 0));
        assertEquals("주문 가격은 0보다 커야 합니다.", e1.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> OrderItem.create(order, 200L, 1, -1000));
        assertEquals("주문 가격은 0보다 커야 합니다.", e2.getMessage());
    }
}