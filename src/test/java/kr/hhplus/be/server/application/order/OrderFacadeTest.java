package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private UserPointService userPointService;

    @Test
    void 주문_프로세스_성공() {
        Long userId = 1L;
        OrderItemCommand req1 = new OrderItemCommand(101L, 2, 15000);
        OrderItemCommand req2 = new OrderItemCommand(102L, 1, 20000);
        List<OrderItemCommand> requestList = List.of(req1, req2);

        CreateOrderCommand command = new CreateOrderCommand(userId,requestList);

        doNothing().when(productService).checkStock(101L, 2);
        doNothing().when(productService).checkStock(102L, 1);

        Order mockOrder = mock(Order.class);
        when(orderService.save(any(Order.class))).thenReturn(mockOrder);
        when(mockOrder.getItems()).thenReturn(List.of(mock(OrderItem.class), mock(OrderItem.class)));
        when(mockOrder.getTotalAmount()).thenReturn(50000);

        // when
        OrderResponse response = orderFacade.processOrder(command);

        // then
        assertEquals(2, response.getItems().size());
        assertEquals(50000, response.getTotalAmount());

        verify(productService).checkStock(101L, 2);
        verify(productService).checkStock(102L, 1);
        verify(orderService).save(any(Order.class));
    }

    @Test
    void 재고_부족으로_주문_실패() {
        Long userId = 1L;
        OrderItemCommand request = new OrderItemCommand(101L, 10, 5000);
        List<OrderItemCommand> requestList = List.of(request);

        CreateOrderCommand command = new CreateOrderCommand(userId,requestList);

        doThrow(new IllegalStateException("상품 재고가 부족합니다. productId=101"))
                .when(productService).checkStock(101L, 10);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderFacade.processOrder(command));

        assertEquals("상품 재고가 부족합니다. productId=101", exception.getMessage());

        verify(productService).checkStock(101L, 10);
        verify(orderService, never()).save(any());
    }

    @Test
    void 주문취소_시_상태변경_정상작동() {
        Long orderId = 1L;
        Order cancelledOrder = mock(Order.class);

        when(cancelledOrder.getStatus()).thenReturn(OrderStatus.CANCEL);
        when(orderService.cancelOrder(orderId)).thenReturn(cancelledOrder);

        Order result = orderFacade.cancelOrder(orderId);

        assertEquals(OrderStatus.CANCEL, result.getStatus());
        verify(orderService).cancelOrder(orderId);
    }

    @Test
    void 사용자별_주문조회() {
        Long userId = 1L;
        Order order1 = mock(Order.class);
        Order order2 = mock(Order.class);

        when(orderService.getOrdersByUser(userId)).thenReturn(List.of(order1, order2));

        List<Order> result = orderFacade.getOrdersByUser(userId);

        assertEquals(2, result.size());
        verify(orderService).getOrdersByUser(userId);
    }
}
