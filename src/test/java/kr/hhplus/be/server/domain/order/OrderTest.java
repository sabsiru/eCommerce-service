package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.application.order.OrderItemCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OrderTest {

    @Test
    void 주문_생성_정상_테스트() {
        Long userId = 1L;

        // 더미 Order 객체 주입 (실제 로직에선 나중에 Order.create()에서 재설정됨)
        OrderItemCommand cmd1 = new OrderItemCommand(101L, 2, 15000);
        OrderItemCommand cmd2 = new OrderItemCommand(102L, 1, 20000);
        List<OrderItemCommand> commands = List.of(cmd1, cmd2);

        Order order = Order.create(userId, commands); // 내부에서 OrderItem 생성 + 연관관계 설정

        // 검증
        assertEquals(userId, order.getUserId());
        assertEquals(2 * 15000 + 1 * 20000, order.getTotalAmount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void 주문_생성_실패_주문항목없음() {
        // when & then
        Long userId = 1L;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Order.create(userId, List.of()));
        assertEquals("주문 항목이 비어 있습니다.", e.getMessage());
    }

    @Test
    void 주문_취소_성공_테스트() {
        // given
        Long userId = 1L;
        OrderItemCommand cmd1 = new OrderItemCommand(101L, 2, 15000);
        List<OrderItemCommand> commands = List.of(cmd1);

        Order order = Order.create(userId, commands); // 내부에서 OrderItem 생성 + 연관관계 설정

        // when
        order.cancel();

        // then
        assertEquals(OrderStatus.CANCEL, order.getStatus());
    }

    @Test
    void 주문아이템_업데이트_정상_동작() throws InterruptedException {
        // given
        OrderItemCommand initialCommand = new OrderItemCommand(10L, 1, 10000);
        Order order = Order.create(1L, List.of(initialCommand));

        LocalDateTime beforeUpdate = order.getUpdatedAt();  // null or 초기값
        Thread.sleep(5);  // updatedAt 변화 유도

        // 새 항목들 준비
        OrderItem item1 = OrderItem.create(order, 101L, 2, 15000);
        OrderItem item2 = OrderItem.create(order, 102L, 1, 20000);
        List<OrderItem> newItems = List.of(item1, item2);

        // when
        order.updateItems(newItems);

        // then
        assertEquals(50000, order.getTotalAmount());
        assertEquals(order, item1.getOrder());
        assertEquals(order, item2.getOrder());
    }
}