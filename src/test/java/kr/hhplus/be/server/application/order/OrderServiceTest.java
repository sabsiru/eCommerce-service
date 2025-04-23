package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    void 주문_생성_성공시_주문항목과_총합이_올바르게_계산되어_저장된다() {
        // given
        Long userId = 1L;
        List<OrderLine> lines = List.of(
                new OrderLine(10L, 2, 15000),
                new OrderLine(20L, 3, 20000)
        );
        Order persisted = new Order(userId);
        persisted.addLine(10L, 2, 15000);
        persisted.addLine(20L, 3, 20000);

        when(orderRepository.save(any(Order.class))).thenReturn(persisted);

        // when
        Order result = orderService.create(userId, lines);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(2, result.getItems().size());
        assertEquals(2 * 15000 + 3 * 20000, result.getTotalAmount());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());
    }

    @Test
    void 주문조회_존재하는ID_성공() {
        // given
        Long orderId = 1L;
        Order existing = new Order(1L);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(existing));

        // when
        Order found = orderService.getOrderOrThrowCancel(orderId);

        // then
        assertSame(existing, found);
        verify(orderRepository).findByIdForUpdate(orderId);
    }

    @Test
    void 주문조회_존재하지않는ID_IllegalArgumentException_발생() {
        // given
        Long invalidId = 99L;
        when(orderRepository.findByIdForUpdate(invalidId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.getOrderOrThrowCancel(invalidId)
        );
        assertEquals("주문을 찾을 수 없습니다. orderId=" + invalidId, ex.getMessage());
    }

    @Test
    void 전체_주문_조회시_리스트가_반환된다() {
        // given
        when(orderRepository.findAll()).thenReturn(List.of(new Order(1L), new Order(2L)));

        // when
        List<Order> all = orderService.getAllOrders();

        // then
        assertEquals(2, all.size());
        verify(orderRepository).findAll();
    }

    @Test
    void 결제처리_Pending상태에서_pay호출시_Status가_PAID로_변경되고_저장된다() {
        // given
        Long orderId = 1L;
        Order order = new Order(1L);
        order.addLine(1L, 1, 10000);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        Order paid = orderService.pay(orderId);

        // then
        assertEquals(OrderStatus.PAID, paid.getStatus());
        verify(orderRepository).findByIdForUpdate(orderId);
        verify(orderRepository).save(order);
        verify(orderItemRepository).saveAll(order.getItems());
    }

    @Test
    void 주문취소_Pending상태에서_cancel호출시_Status가_CANCEL로_변경되고_저장된다() {
        // given
        Long orderId = 1L;
        Order order = new Order(1L);
        order.addLine(1L, 1, 10000);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        Order canceled = orderService.cancel(orderId);

        // then
        assertEquals(OrderStatus.CANCEL, canceled.getStatus());
        verify(orderRepository).findByIdForUpdate(orderId);
        verify(orderRepository).save(order);
        verify(orderItemRepository).saveAll(order.getItems());
    }

    @Test
    void 주문항목업데이트_새항목으로_교체되고_총합이_재계산되어_저장된다() {
        // given
        Long orderId = 1L;
        Order order = new Order(1L);
        order.addLine(1L, 1, 10000);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        List<OrderItem> newItems = List.of(
                OrderItem.of(order, 101L, 2, 15000),
                OrderItem.of(order, 102L, 1, 20000)
        );

        // when
        Order updated = orderService.updateOrderItems(orderId, newItems);

        // then
        assertEquals(2, updated.getItems().size());
        assertEquals(2*15000 + 1*20000, updated.getTotalAmount());
        verify(orderRepository).findByIdForUpdate(orderId);
        verify(orderRepository).save(updated);
        verify(orderItemRepository).saveAll(updated.getItems());
    }

    @Test
    void 사용자별주문조회_존재하면_리스트반환된다() {
        // given
        Long userId = 1L;
        List<Order> orders = List.of(new Order(userId), new Order(userId));
        when(orderRepository.findByUserId(userId)).thenReturn(orders);

        // when
        List<Order> result = orderService.getOrdersByUser(userId);

        // then
        assertEquals(2, result.size());
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    void 사용자별주문조회_없으면_IllegalArgumentException_발생() {
        // given
        Long userId = 2L;
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.getOrdersByUser(userId)
        );
        assertEquals("해당 유저가 없거나 주문 목록이 없습니다.", ex.getMessage());
    }
}
