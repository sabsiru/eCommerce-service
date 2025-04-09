package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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
        Long userCouponId = 2L;

        // 주문 항목 생성: item1: 2개, 단가 15,000 -> 총액 30,000, item2: 3개, 단가 20,000 -> 총액 60,000
        OrderItem item1 = OrderItem.create(100L, 10L, 2, 15000);
        OrderItem item2 = OrderItem.create(100L, 20L, 3, 20000);
        List<OrderItem> items = Arrays.asList(item1, item2);
        // 총 주문 금액은 30,000 + 60,000 = 90,000

        Order orderToCreate = Order.create(userId, items);
        // Stub save: repository.save() 가 생성된 orderToCreate를 반환하도록 설정
        when(orderRepository.save(any(Order.class))).thenReturn(orderToCreate);

        // when
        Order createdOrder = orderService.createOrder(userId, items);

        // then
        assertNotNull(createdOrder);
        assertNull(createdOrder.id());
        assertEquals(userId, createdOrder.userId());
        assertEquals(orderToCreate.calculateTotalAmount(), createdOrder.totalAmount());
        assertEquals(OrderStatus.PENDING, createdOrder.status());
        assertEquals(2, createdOrder.items().size());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void 단건주문조회_성공_테스트() {
        // given
        Long orderId = 1L;
        // 간단한 주문 생성 (주문 항목은 한 건으로 처리)
        OrderItem item = OrderItem.create(100L, 10L, 2, 15000);
        Order order = Order.create(1L, List.of(item));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when
        Order foundOrder = orderService.getOrderOrThrow(orderId);

        // then
        assertNotNull(foundOrder);
        assertEquals(order.userId(), foundOrder.userId());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void 단건주문조회_실패_테스트() {
        // given
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderOrThrow(orderId));
        assertEquals("주문을 찾을 수 없습니다. orderId=" + orderId, e.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void 모든주문조회_테스트() {
        // given
        OrderItem item = OrderItem.create(100L, 10L, 2, 15000);
        Order order1 = Order.create(1L, List.of(item));
        Order order2 = Order.create(2L, List.of(item));
        List<Order> orders = Arrays.asList(order1, order2);
        when(orderRepository.findAll()).thenReturn(orders);

        // when
        List<Order> result = orderService.getAllOrders();

        // then
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }


    @Test
    void 주문취소_성공_테스트() {
        // given
        OrderItem item = OrderItem.create(100L, 10L, 2, 15000);
        Order order = Order.create(1L, List.of(item));
        Order cancelledOrder = order.cancel();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // when
        Order result = orderService.cancelOrder(1L);

        // then
        assertEquals(OrderStatus.CANCEL, result.status());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }


    @Test
    void 주문_상품_업데이트_성공() throws InterruptedException {
        // Given: 기존 주문 (Order) 생성. 주문 항목은 빈 리스트로 생성하고 총액 0.
        Long orderId = 1L;
        Long userId = 100L;
        Long userCouponId = 10L;
        OrderStatus status = OrderStatus.PENDING;
        LocalDateTime now = LocalDateTime.now();
        Order originalOrder = new Order(orderId, userId, Collections.emptyList(), 0, status, now, now);

        // 새 주문 항목 2건 생성
        // OrderItem.create()는 주문 항목 생성 시 orderId를 필수로 받으므로, 기존 주문의 id(orderId)를 넣어줌.
        OrderItem item1 = OrderItem.create(orderId, 101L, 2, 15000); // 총액 2 * 15000 = 30000
        OrderItem item2 = OrderItem.create(orderId, 102L, 1, 20000); // 총액 1 * 20000 = 20000
        List<OrderItem> newItems = Arrays.asList(item1, item2);

        // 재계산된 총액은 30000 + 20000 = 50000
        int expectedTotalAmount = 50000;

        // 약간의 지연: updatedAt 값의 변경을 확인하기 위해
        Thread.sleep(10);
        // updateItems 메서드 내부에서 새로운 Order 객체를 생성할 때 updatedAt이 갱신됨.
        Order updatedOrder = originalOrder.updateItems(newItems);

        // Stub: OrderRepository에서 orderId로 조회된 기존 주문과 save() 호출 시 새로운 업데이트된 Order 객체 반환
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(originalOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        // When: OrderService.updateOrderItems() 호출
        Order result = orderService.updateOrderItems(orderId, newItems);

        // Then:
        // 1. 업데이트된 주문의 주문 항목 목록(items)이 newItems와 동일해야 한다.
        assertEquals(newItems, result.items());

        // 2. 총 주문 금액(totalAmount)이 newItems의 totalPrice 합계와 일치해야 한다.
        assertEquals(expectedTotalAmount, result.totalAmount() );

        // 3. updatedAt이 원래의 updatedAt보다 나중 시간이어야 함 (갱신 확인)
        assertTrue(result.updatedAt().isAfter(now));

        // 4. OrderRepository의 findById와 save 메서드가 각각 한번씩 호출되었는지 검증
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void 사용자별주문조회_정상반환() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();
        // Order 생성: 가정 - id, userId, userCouponId, items, totalAmount, status, createdAt, updatedAt
        Order order1 = new Order(1L, userId, Arrays.asList(), 0, OrderStatus.PENDING, now, now);
        Order order2 = new Order(2L, userId, Arrays.asList(), 0, OrderStatus.PENDING, now, now);
        List<Order> expectedOrders = Arrays.asList(order1, order2);

        // Stub 처리: userId에 해당하는 주문 목록을 반환
        when(orderRepository.findByUserId(userId)).thenReturn(expectedOrders);

        // when
        List<Order> actualOrders = orderService.getOrdersByUser(userId);

        // then
        assertEquals(expectedOrders, actualOrders);
        verify(orderRepository, times(1)).findByUserId(userId);
    }
}