package kr.hhplus.be.server.application.order;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertThrows;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void 주문_생성_성공() {
        // given
        Product p1 = productRepository.save(new Product("상품1", 1000, 10, 1L));
        Product p2 = productRepository.save(new Product("상품2", 2000, 10, 1L));

        OrderItemCommand cmd1 = new OrderItemCommand(p1.getId(), 2, p1.getPrice());
        OrderItemCommand cmd2 = new OrderItemCommand(p2.getId(), 1, p2.getPrice());
        List<OrderItemCommand> commands = List.of(cmd1, cmd2);

        // when
        Order order = Order.create(1L, commands);
        Order saved = orderRepository.save(order);

        // then
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.calculateTotalAmount()).isEqualTo(1000 * 2 + 2000);
    }
    @Test
    void 단건_주문_조회_성공() {
        OrderItemCommand cmd = new OrderItemCommand(101L, 1, 10000);
        Order order = Order.create(1L, List.of(cmd));
        Order saved = orderRepository.save(order);

        Order found = orderService.getOrderOrThrow(saved.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    void 단건_주문_조회_실패() {
        Long invalidId = 999L;

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderOrThrow(invalidId));

        assertThat(e.getMessage()).isEqualTo("주문을 찾을 수 없습니다. orderId=" + invalidId);
    }

    @Test
    void 전체_주문_조회_성공() {
        Order order1 = orderRepository.save(Order.create(1L, List.of(new OrderItemCommand(101L, 1, 10000))));
        Order order2 = orderRepository.save(Order.create(2L, List.of(new OrderItemCommand(102L, 2, 5000))));

        List<Order> result = orderService.getAllOrders();

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void 주문_취소_성공() {
        Order order = orderRepository.save(Order.create(1L, List.of(new OrderItemCommand(101L, 1, 10000))));

        Order result = orderService.cancelOrder(order.getId());

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCEL);
    }

    @Test
    void 주문_항목_업데이트_성공() {
        Order order = orderRepository.save(Order.create(1L, List.of(new OrderItemCommand(101L, 1, 10000))));

        List<OrderItemCommand> newItems = List.of(
                new OrderItemCommand(201L, 2, 15000),
                new OrderItemCommand(202L, 1, 20000)
        );
        List<OrderItem> converted = newItems.stream()
                .map(c -> OrderItem.create(order, c.getProductId(), c.getQuantity(), c.getOrderPrice()))
                .toList();

        Order updated = orderService.updateOrderItems(order.getId(), converted);

        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getTotalAmount()).isEqualTo(2 * 15000 + 1 * 20000);
    }

    @Test
    void 사용자별_주문_조회() {
        Long userId = 99L;
        orderRepository.save(Order.create(userId, List.of(new OrderItemCommand(101L, 1, 10000))));
        orderRepository.save(Order.create(userId, List.of(new OrderItemCommand(102L, 2, 5000))));

        List<Order> result = orderService.getOrdersByUser(userId);

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result).allMatch(order -> order.getUserId().equals(userId));
    }
}
