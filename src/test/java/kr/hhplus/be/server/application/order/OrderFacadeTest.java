package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.dto.OrderItemRequest;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private ProductService productService;

    @Test
    void 주문_프로세스_성공() {
        // Given
        Long userId = 1L;

        // 주문 항목 생성에 필요한 파라미터 (OrderItemRequest DTO)
        OrderItemRequest param1 = new OrderItemRequest(101L,2, 15000); // 총액: 2 * 15000 = 30000
        OrderItemRequest param2 = new OrderItemRequest(102L,1, 20000); // 총액: 1 * 20000 = 20000
        List<OrderItemRequest> params = Arrays.asList(param1, param2);

        // 재고 확인: 각 상품의 재고가 충분하다고 가정 (예: 50씩)
        when(productService.getStock(101L)).thenReturn(50);
        when(productService.getStock(102L)).thenReturn(50);

        // 주문 생성: 빈 주문 항목 리스트로 주문 생성 → 주문 ID 할당 (예, order.id=1L)
        LocalDateTime now = LocalDateTime.now();
        Order initialOrder = new Order(1L, userId, new ArrayList<>(), 0, OrderStatus.PENDING, now, now);
        when(orderService.createOrder(userId, new ArrayList<>()))
                .thenReturn(initialOrder);

        // 주문 항목 생성: 생성된 주문 ID를 사용하여 OrderItem을 생성
        OrderItem createdItem1 = OrderItem.create(initialOrder.id(), 101L, 2, 15000);
        OrderItem createdItem2 = OrderItem.create(initialOrder.id(), 102L, 1, 20000);
        when(orderItemService.createOrderItem(initialOrder.id(), 101L, 2, 15000))
                .thenReturn(createdItem1);
        when(orderItemService.createOrderItem(initialOrder.id(), 102L, 1, 20000))
                .thenReturn(createdItem2);

        // 주문 항목 업데이트: 생성된 주문 항목 리스트로 주문 업데이트
        List<OrderItem> updatedItems = Arrays.asList(createdItem1, createdItem2);
        int expectedTotal = createdItem1.totalPrice() + createdItem2.totalPrice(); // 30000 + 20000 = 50000
        // 여기서는 updatedOrder의 updatedAt를 원래 now보다 나중이라 가정
        Order updatedOrder = new Order(initialOrder.id(), userId, updatedItems, expectedTotal, OrderStatus.PENDING, now, now.plusSeconds(1));
        when(orderService.updateOrderItems(initialOrder.id(), updatedItems))
                .thenReturn(updatedOrder);

        // When
        Order finalOrder = orderFacade.processOrder(userId, params);

        // Then
        assertEquals(expectedTotal, finalOrder.totalAmount(), "총 주문 금액이 올바르게 계산되어야 합니다.");
        assertEquals(2, finalOrder.items().size(), "주문 항목 수가 2건이어야 합니다.");

        // 각 서비스 호출 검증
        verify(productService, times(1)).getStock(101L);
        verify(productService, times(1)).getStock(102L);
        verify(orderService, times(1)).createOrder(userId, new ArrayList<>());
        verify(orderItemService, times(1)).createOrderItem(initialOrder.id(), 101L, 2, 15000);
        verify(orderItemService, times(1)).createOrderItem(initialOrder.id(), 102L, 1, 20000);
        verify(orderService, times(1)).updateOrderItems(initialOrder.id(), updatedItems);
    }

    @Test
    void 주문_프로세스_재고부족시_생성실패() {
        // Given
        Long userId = 1L;
        Long userCouponId = 10L;

        OrderItemRequest param1 = new OrderItemRequest(101L, 2, 15000);
        List<OrderItemRequest> params = Arrays.asList(param1);

        // 재고 확인: 상품 101L의 재고를 1로 설정하여, 주문 수량(2)보다 부족함
        when(productService.getStock(101L)).thenReturn(1);

        // When & Then: processOrder 호출 시 재고 부족 예외가 발생해야 함
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderFacade.processOrder(userId, params));
        assertEquals("상품 재고가 부족합니다. productId=101", ex.getMessage());

        // 다른 서비스는 호출되지 않아야 함
        verify(orderService, never()).createOrder(anyLong(), any());
        verify(orderItemService, never()).createOrderItem(anyLong(), anyLong(), anyInt(), anyInt());
        verify(orderService, never()).updateOrderItems(anyLong(), any());
    }




}