package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.*;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserPointService;
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
    private UserPointService userPointService;

    @Test
    void 주문_성공시_상품재고확인과_주문생성_흐름이_정상작동한다() {
        // given
        Long userId = 1L;
        OrderCommand.Item item1 = new OrderCommand.Item(101L, 2, 15000);
        OrderCommand.Item item2 = new OrderCommand.Item(102L, 1, 20000);
        OrderCommand.Create command = new OrderCommand.Create(userId, List.of(item1, item2));

        User mockUser = User.create("테스터", 100000);
        when(userPointService.getUserOrThrow(userId)).thenReturn(mockUser);

        doNothing().when(productService).checkStock(101L, 2);
        doNothing().when(productService).checkStock(102L, 1);


        Order dummyOrder = new Order(userId);
        dummyOrder.addLine(101L, 2, 15000);
        dummyOrder.addLine(102L, 1, 20000);
        when(orderService.create(eq(userId), anyList())).thenReturn(dummyOrder);

        // when
        OrderResult.Create result = orderFacade.processOrder(command);

        // then
        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals(50000, result.getTotalPrice());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        verify(productService).checkStock(101L, 2);
        verify(productService).checkStock(102L, 1);
        verify(orderService).create(eq(userId), anyList());
    }

    @Test
    void 상품재고가_부족하면_IllegalStateException이_발생한다() {
        // given
        Long userId = 1L;
        OrderCommand.Item item = new OrderCommand.Item(101L, 10, 5000);
        OrderCommand.Create command = new OrderCommand.Create(userId, List.of(item));

        doThrow(new IllegalStateException("상품 재고가 부족합니다. productId=101"))
                .when(productService).checkStock(101L, 10);

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                orderFacade.processOrder(command)
        );

        assertEquals("상품 재고가 부족합니다. productId=101", e.getMessage());
        verify(productService).checkStock(101L, 10);
        verify(orderService, never()).save(any());
    }

    @Test
    void 주문을_취소하면_상태가_CANCEL로_변경된다() {
        Long orderId = 1L;
        Long userId = 1L;
        Order orderBefore = new Order(userId);
        orderBefore.addLine(100L, 1, 10000);
        Order canceled = new Order(userId);
        canceled.addLine(100L, 1, 10000);
        canceled.cancel();

        when(orderService.cancel(orderId)).thenReturn(canceled);

        OrderResult.Create result = orderFacade.cancelOrder(orderId);

        assertEquals(OrderStatus.CANCEL, result.getStatus());
        assertEquals(10000, result.getTotalPrice());
        verify(orderService).cancel(orderId);
    }

    @Test
    void 사용자별_주문목록을_정상적으로_반환한다() {
        // given
        Long userId = 1L;
        Order o1 = new Order(userId);
        o1.addLine(10L, 1, 10000);
        Order o2 = new Order(userId);
        o2.addLine(20L, 2, 15000);
        when(orderService.getOrdersByUser(userId)).thenReturn(List.of(o1, o2));

        List<OrderResult.Create> result = orderFacade.getOrdersByUser(userId);

        assertEquals(2, result.size());
        assertEquals(10000, result.get(0).getTotalPrice());
        assertEquals(30000, result.get(1).getTotalPrice());
        verify(orderService).getOrdersByUser(userId);
    }
}