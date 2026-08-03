package kr.hhplus.be.server.application.order;

import jakarta.persistence.EntityManager;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void 주문_생성_성공시_OrderResult가_반환된다() {
        // given
        User user = userRepository.save(User.create("테스터", 0));
        Product p1 = productRepository.save(new Product("상품1", 15000, 100, user.getId()));
        Product p2 = productRepository.save(new Product("상품2", 20000, 100, user.getId()));

        // when
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
    void 주문_생성시_클라이언트가_보낸_가격은_무시되고_실제_상품가격이_적용된다() {
        // given
        User user = userRepository.save(User.create("테스터", 0));
        Product product = productRepository.save(new Product("상품", 15000, 100, user.getId()));

        // 클라이언트가 실제 가격(15000)과 다른 조작된 가격(1원)을 보냄
        OrderCommand.Create cmd = new OrderCommand.Create(
                user.getId(),
                List.of(new OrderCommand.Item(product.getId(), 2, 1))
        );

        // when
        OrderResult.Create result = orderFacade.processOrder(cmd);

        // then: 조작된 가격(1원 * 2 = 2)이 아니라 실제 상품 가격(15000 * 2)이 적용돼야 한다
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getItemPrice()).isEqualTo(15000);
        assertThat(result.getTotalPrice()).isEqualTo(2 * 15000);
    }

    @Test
    void 주문_취소_성공시_Status가_CANCEL로_변경된다() {
        // given
        User user = userRepository.save(User.create("테스터", 0));
        Product p = productRepository.save(new Product("상품", 10000, 100, user.getId()));
        OrderCommand.Create createCmd = new OrderCommand.Create(
                user.getId(),
                List.of(new OrderCommand.Item(p.getId(), 1, p.getPrice()))
        );
        OrderResult.Create created = orderFacade.processOrder(createCmd);

        // 실제 운영에서는 주문 생성과 취소가 서로 다른 트랜잭션(별개의 HTTP 요청)에서
        // 일어난다. 같은 트랜잭션 안에서는 Hibernate 영속성 컨텍스트가 이미 로드된
        // Order 인스턴스를 그대로 재사용해서 @Transient인 items 필드가 우연히 채워진
        // 것처럼 보일 수 있으므로, 영속성 컨텍스트를 비워 진짜 재조회 상황을 재현한다.
        entityManager.clear();

        // when
        OrderResult.Create canceled = orderFacade.cancelOrder(created.getOrderId());

        // then
        assertThat(canceled.getStatus()).isEqualTo(OrderStatus.CANCEL);
        assertThat(canceled.getItems()).hasSize(1);
        assertThat(canceled.getItems().get(0).getProductId()).isEqualTo(p.getId());
        assertThat(canceled.getItems().get(0).getQuantity()).isEqualTo(1);
        assertThat(canceled.getTotalPrice()).isEqualTo(p.getPrice());
    }

    @Test
    void 사용자별_주문조회_성공시_리스트를_반환한다() {
        // given
        User user = userRepository.save(User.create("테스터", 0));
        Product p = productRepository.save(new Product("상품", 5000, 100, user.getId()));
        OrderCommand.Create cmd = new OrderCommand.Create(
                user.getId(),
                List.of(new OrderCommand.Item(p.getId(), 3, p.getPrice()))
        );
        orderFacade.processOrder(cmd);
        orderFacade.processOrder(cmd);

        // 실제 운영에서는 조회가 별도의 HTTP 요청(별도 트랜잭션)에서 일어난다.
        // 영속성 컨텍스트를 비워서 진짜 재조회 상황을 재현한다 (아래 설명 참조).
        entityManager.clear();

        // when
        List<OrderResult.Create> list = orderFacade.getOrdersByUser(user.getId());

        // then
        assertThat(list).hasSize(2);
        list.forEach(r -> {
            assertThat(r.getUserId()).isEqualTo(user.getId());
            assertThat(r.getItems()).hasSize(1);
            assertThat(r.getItems().get(0).getProductId()).isEqualTo(p.getId());
            assertThat(r.getItems().get(0).getQuantity()).isEqualTo(3);
            assertThat(r.getTotalPrice()).isEqualTo(3 * p.getPrice());
        });
    }

    @Test
    void 사용자별_주문조회_없을경우_IllegalArgumentException_발생() {
        // given
        Long invalidUser = 9999L;

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> orderFacade.getOrdersByUser(invalidUser)
        );
    }
}
