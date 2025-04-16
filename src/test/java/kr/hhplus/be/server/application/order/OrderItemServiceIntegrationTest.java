package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderItemServiceIntegrationTest {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderService orderService;

    @Test
    void 주문아이템_생성_성공() {
        OrderItemCommand itemCommand = new OrderItemCommand(999L, 1, 1);
        CreateOrderCommand command = new CreateOrderCommand();
        command.setUserId(1L);
        command.setOrderItemCommands(List.of(itemCommand));

        Order order = orderService.createOrder(command);

        OrderItem item = orderItemService.createOrderItem(order, 101L, 2, 15000);

        assertThat(item.getOrder()).isEqualTo(order);
        assertThat(item.getProductId()).isEqualTo(101L);
        assertThat(item.totalPrice()).isEqualTo(2 * 15000);
    }

    @Test
    void 주문아이템_ID로_조회_성공() {
        OrderItemCommand itemCommand = new OrderItemCommand(999L, 1, 1);
        CreateOrderCommand command = new CreateOrderCommand();
        command.setUserId(1L);
        command.setOrderItemCommands(List.of(itemCommand));

        Order order = orderService.createOrder(command);
        OrderItem item = orderItemService.createOrderItem(order, 101L, 2, 15000);

        OrderItem found = orderItemService.getOrderItemById(item.getId());

        assertThat(found.getId()).isEqualTo(item.getId());
    }

    @Test
    void 주문아이템_ID로_조회_실패() {
        Long invalidId = 999L;

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> orderItemService.getOrderItemById(invalidId));

        assertThat(e.getMessage()).isEqualTo("주문 항목을 찾을 수 없습니다.");
    }

    @Test
    void 주문기반_아이템_전체조회() {
        // given
        OrderItemCommand item1 = new OrderItemCommand(101L, 2, 15000);
        OrderItemCommand item2 = new OrderItemCommand(102L, 1, 20000);

        CreateOrderCommand command = new CreateOrderCommand();
        command.setUserId(1L);
        command.setOrderItemCommands(List.of(item1, item2));

        Order order = orderService.createOrder(command); // 이 시점에 2개의 OrderItem 생성됨

        // when
        List<OrderItem> result = orderItemService.getOrderItemsByOrder(order);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(item -> item.getOrder().equals(order));
    }
}
