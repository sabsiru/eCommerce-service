package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderFacadeIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void 주문_생성_성공() {
        // given - 상품 등록 먼저
        productRepository.save(new Product("상품1", 15000, 100, 1L));
        productRepository.save(new Product("상품2", 20000, 100, 1L));

        OrderItemCommand item1 = new OrderItemCommand(1L, 2, 15000);
        OrderItemCommand item2 = new OrderItemCommand(2L, 1, 20000);
        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(item1, item2));

        // when
        OrderResponse response = orderFacade.processOrder(command);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalAmount()).isEqualTo(2 * 15000 + 1 * 20000);
    }

    @Test
    void 주문_취소_정상작동() {
        OrderItemCommand item = new OrderItemCommand(101L, 1, 10000);
        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(item));
        Order order = orderRepository.save(Order.create(command.getUserId(), command.getOrderItemCommands()));

        Order cancelled = orderFacade.cancelOrder(order.getId());

        assertThat(cancelled.getStatus().name()).isEqualTo("CANCEL");
    }

    @Test
    void 사용자별_주문_조회() {
        User userId = userRepository.save(User.create("테스터", 0));
        CreateOrderCommand command = new CreateOrderCommand(userId.getId(), List.of(
                new OrderItemCommand(101L, 1, 10000)));
        orderRepository.save(Order.create(userId.getId(), command.getOrderItemCommands()));

        List<Order> orders = orderFacade.getOrdersByUser(userId.getId());

        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getUserId()).isEqualTo(userId.getId());
    }

    @Test
    void 주문_생성_실패_재고부족() {
        // given: 재고가 1인 상품 등록
        Product product = productRepository.save(new Product("상품", 10000, 1, 1L));

        // 주문 수량은 999로 재고 초과
        OrderItemCommand command = new OrderItemCommand(product.getId(), 999, product.getPrice());
        CreateOrderCommand createCommand = new CreateOrderCommand(1L, List.of(command));

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> orderFacade.processOrder(createCommand));

        assertThat(e.getMessage()).contains("상품 재고가 부족합니다.");
    }
}
