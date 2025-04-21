package kr.hhplus.be.server.domain.order;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void 생성시_userId와_Pending_상태_확인() {
        Order order = new Order(42L);
        assertEquals(42L, order.getUserId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0, order.getTotalAmount());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void addLine_여러번_호출시_총액_합산() {
        Order order = new Order(1L);
        order.addLine(100L, 2, 15000);
        order.addLine(200L, 1, 20000);
        assertEquals(2 * 15000 + 1 * 20000, order.getTotalAmount());
        assertEquals(2, order.getItems().size());
    }

    @Test
    void pay_성공_and_status_변경() {
        Order order = new Order(1L);
        order.addLine(1L,1,1000);
        order.pay();
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void pay_실패_if_not_PENDING() {
        Order order = new Order(1L);
        order.addLine(1L,1,1000);
        order.pay();
        IllegalStateException e = assertThrows(IllegalStateException.class, order::pay);
        assertEquals("결제는 PENDING 상태의 주문에만 가능합니다.", e.getMessage());
    }

    @Test
    void cancel_성공_and_status_변경() {
        Order order = new Order(1L);
        order.addLine(1L,1,1000);
        order.cancel();
        assertEquals(OrderStatus.CANCEL, order.getStatus());
    }

    @Test
    void cancel_실패_if_not_PENDING() {
        Order order = new Order(1L);
        order.addLine(1L,1,1000);
        order.pay();
        IllegalStateException e = assertThrows(IllegalStateException.class, order::cancel);
        assertEquals("이미 결제 완료된 주문은 취소할 수 없습니다.", e.getMessage());
    }

    @Test
    void updateItems_교체후_총액_재계산() {
        Order order = new Order(1L);
        order.addLine(1L,1,1000);
        List<OrderItem> newItems = List.of(
                OrderItem.of(order, 101L,2,1500),
                OrderItem.of(order, 102L,1,2000)
        );
        order.updateItems(newItems);
        assertEquals(2*1500 + 1*2000, order.getTotalAmount());
    }
}