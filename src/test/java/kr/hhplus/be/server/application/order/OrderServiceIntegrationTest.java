package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.*;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void 주문_생성_성공시_Order가_저장되고_총합이_계산된다() {
        // given
        Product p1 = productRepository.save(new Product("상품1", 1000, 10, 1L));
        Product p2 = productRepository.save(new Product("상품2", 2000, 10, 1L));
        List<OrderLine> lines = List.of(
                new OrderLine(p1.getId(), 2, p1.getPrice()),
                new OrderLine(p2.getId(), 1, p2.getPrice())
        );

        // when
        Order order = orderService.create(1L, lines);

        // then
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.calculateTotalAmount()).isEqualTo(2 * p1.getPrice() + p2.getPrice());
    }

    @Test
    void 단건_주문조회_성공시_Order반환() {
        // given
        Product p = productRepository.save(new Product("상품", 10000, 10, 1L));
        List<OrderLine> lines = List.of(new OrderLine(p.getId(), 1, p.getPrice()));
        Order saved = orderService.create(1L, lines);

        // when
        Order found = orderService.getOrderOrThrowCancel(saved.getId());

        // then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    void 단건_주문조회_실패시_IllegalArgumentException_발생() {
        // when & then
        Long invalidId = 999L;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderOrThrowCancel(invalidId)
        );
        assertThat(e.getMessage()).isEqualTo("주문을 찾을 수 없습니다. orderId=" + invalidId);
    }

    @Test
    void 전체_주문조회_성공시_리스트반환() {
        // given two orders
        Product p = productRepository.save(new Product("상품", 5000, 10, 1L));
        orderService.create(1L, List.of(new OrderLine(p.getId(), 1, p.getPrice())));
        orderService.create(2L, List.of(new OrderLine(p.getId(), 2, p.getPrice())));

        // when
        List<Order> result = orderService.getAllOrders();

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void 주문_취소_성공시_Status가_CANCEL로_변경된다() {
        // given
        Product p = productRepository.save(new Product("상품", 8000, 10, 1L));
        Order saved = orderService.create(1L, List.of(new OrderLine(p.getId(), 1, p.getPrice())));

        // when
        Order canceled = orderService.cancel(saved.getId());

        // then
        assertThat(canceled.getStatus()).isEqualTo(OrderStatus.CANCEL);
    }

    @Test
    void 주문항목_업데이트_성공시_새항목으로_교체되고_총합재계산된다() {
        // given
        Product pOld = productRepository.save(new Product("구상품", 10000, 10, 1L));
        Order saved = orderService.create(1L, List.of(new OrderLine(pOld.getId(), 1, pOld.getPrice())));

        Product pNew1 = productRepository.save(new Product("신상품1", 12000, 10, 1L));
        Product pNew2 = productRepository.save(new Product("신상품2", 15000, 10, 1L));
        List<OrderItem> newItems = List.of(
                new OrderItem(saved, pNew1.getId(), 2, pNew1.getPrice()),
                new OrderItem(saved, pNew2.getId(), 1, pNew2.getPrice())
        );

        // when
        Order updated = orderService.updateOrderItems(saved.getId(), newItems);

        // then
        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getTotalAmount()).isEqualTo(2 * pNew1.getPrice() + pNew2.getPrice());
    }

    @Test
    void 사용자별_주문조회_성공시_회원ID일치() {
        // given
        Long userId = 99L;
        Product p = productRepository.save(new Product("회원상품", 7000, 10, 1L));
        orderService.create(userId, List.of(new OrderLine(p.getId(), 1, p.getPrice())));
        orderService.create(userId, List.of(new OrderLine(p.getId(), 2, p.getPrice())));

        // when
        List<Order> result = orderService.getOrdersByUser(userId);

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(2)
                .allMatch(o -> o.getUserId().equals(userId));
    }
}