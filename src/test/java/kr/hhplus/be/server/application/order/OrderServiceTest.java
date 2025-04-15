package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Test
    void 주문_생성_다중항목_테스트() {
        // given
        Long userId = 1L;
        OrderItemCommand cmd1 = new OrderItemCommand(10L, 2, 15000);
        OrderItemCommand cmd2 = new OrderItemCommand(20L, 3, 20000);

        Order tempOrder = Order.create(userId, List.of(cmd1, cmd2));
        when(orderRepository.save(any(Order.class))).thenReturn(tempOrder);

        // when
        Order result = orderService.save(tempOrder);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(90000, result.getTotalAmount());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(2, result.getItems().size());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void 단건주문조회_성공_테스트() {
        // given
        Long orderId = 1L;
        OrderItemCommand cmd = new OrderItemCommand(10L, 1, 10000);
        Order order = Order.create(1L, List.of(cmd));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when
        Order found = orderService.getOrderOrThrow(orderId);

        // then
        assertNotNull(found);
        assertEquals(order.getUserId(), found.getUserId());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void 단건주문조회_실패_테스트() {
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderOrThrow(orderId));
        assertEquals("주문을 찾을 수 없습니다. orderId=" + orderId, e.getMessage());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void 모든주문조회_테스트() {
        Order order1 = Order.create(1L, List.of(new OrderItemCommand(10L, 1, 10000)));
        Order order2 = Order.create(2L, List.of(new OrderItemCommand(11L, 1, 12000)));

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getAllOrders();

        assertEquals(2, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    void 주문취소_성공_테스트() {
        Long orderId = 1L;
        Order order = Order.create(1L, List.of(new OrderItemCommand(10L, 1, 10000)));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.cancelOrder(orderId);

        assertEquals(OrderStatus.CANCEL, result.getStatus());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void 주문_상품_업데이트_성공() throws InterruptedException {
        // given
        Long orderId = 1L;
        Order originalOrder = Order.create(1L, List.of(new OrderItemCommand(10L, 1, 10000)));
        Thread.sleep(5);

        List<OrderItem> newItems = List.of(
                OrderItem.create(originalOrder, 101L, 2, 15000),
                OrderItem.create(originalOrder, 102L, 1, 20000)
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(originalOrder));

        // when
        Order result = orderService.updateOrderItems(orderId, newItems);

        // then
        assertEquals(newItems.size(), result.getItems().size());
        assertEquals(50000, result.getTotalAmount());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void 사용자별주문조회_정상반환() {
        Long userId = 1L;
        Order order1 = Order.create(userId, List.of(new OrderItemCommand(10L, 1, 10000)));
        Order order2 = Order.create(userId, List.of(new OrderItemCommand(20L, 2, 15000)));
        List<Order> expected = List.of(order1, order2);

        when(orderRepository.findByUserId(userId)).thenReturn(expected);

        List<Order> actual = orderService.getOrdersByUser(userId);

        assertEquals(expected, actual);
        verify(orderRepository).findByUserId(userId);
    }
}