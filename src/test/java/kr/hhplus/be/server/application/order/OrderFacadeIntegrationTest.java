package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderFacadeIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void 주문_생성_성공시_OrderResult가_반환된다() {
        // given: 사용자 및 상품 등록
        User user = userRepository.save(User.create("테스터", 0));
        Product p1 = productRepository.save(new Product("상품1", 15000, 100, user.getId()));
        Product p2 = productRepository.save(new Product("상품2", 20000, 100, user.getId()));

        // when: 주문 생성
        OrderCommand.Create cmd = new OrderCommand.Create(
                user.getId(),
                List.of(
                        new OrderCommand.Item(p1.getId(), 2, p1.getPrice()),
                        new OrderCommand.Item(p2.getId(), 1, p2.getPrice())
                )
        );
        OrderResult.Create result = orderFacade.processOrder(cmd);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalPrice()).isEqualTo(2 * p1.getPrice() + 1 * p2.getPrice());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void 주문_취소_성공시_Status가_CANCEL로_변경된다() {
        // given: 먼저 주문 생성
        User user = userRepository.save(User.create("테스터", 0));
        Product p = productRepository.save(new Product("상품", 10000, 100, user.getId()));
        OrderCommand.Create createCmd = new OrderCommand.Create(
                user.getId(),
                List.of(new OrderCommand.Item(p.getId(), 1, p.getPrice()))
        );
        OrderResult.Create created = orderFacade.processOrder(createCmd);

        // when: 주문 취소
        OrderResult.Create canceled = orderFacade.cancelOrder(created.getOrderId());

        // then
        assertThat(canceled.getStatus()).isEqualTo(OrderStatus.CANCEL);
        assertThat(canceled.getTotalPrice()).isEqualTo(p.getPrice());
    }

    @Test
    void 사용자별_주문조회_성공시_리스트를_반환한다() {
        // given: 두 건의 주문 생성
        User user = userRepository.save(User.create("테스터", 0));
        Product p = productRepository.save(new Product("상품", 5000, 100, user.getId()));
        OrderCommand.Create cmd = new OrderCommand.Create(
                user.getId(),
                List.of(new OrderCommand.Item(p.getId(), 3, p.getPrice()))
        );
        orderFacade.processOrder(cmd);
        orderFacade.processOrder(cmd);

        // when
        List<OrderResult.Create> list = orderFacade.getOrdersByUser(user.getId());

        // then
        assertThat(list).hasSize(2);
        list.forEach(r -> {
            assertThat(r.getUserId()).isEqualTo(user.getId());
            assertThat(r.getTotalPrice()).isEqualTo(3 * p.getPrice());
        });
    }

    @Test
    void 사용자별_주문조회_없을경우_IllegalArgumentException_발생() {
        // given: 존재하지 않는 사용자 ID
        Long invalidUser = 9999L;

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> orderFacade.getOrdersByUser(invalidUser)
        );
    }
}
