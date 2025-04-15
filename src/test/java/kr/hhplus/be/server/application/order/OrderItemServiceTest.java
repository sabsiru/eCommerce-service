package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @InjectMocks
    private OrderItemService orderItemService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    void 주문_아이템_정상_추가() {
        // given
        Order order = Order.create(1L, List.of(new OrderItemCommand(999L, 1, 1)));
        Long productId = 10L;
        int quantity = 2;
        int orderPrice = 15000;
        OrderItem orderItem = OrderItem.create(order, productId, quantity, orderPrice);

        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);

        // when
        OrderItem savedItem = orderItemService.createOrderItem(order, productId, quantity, orderPrice);

        // then
        assertNotNull(savedItem);
        assertEquals(productId, savedItem.getProductId());
        assertEquals(quantity, savedItem.getQuantity());
        assertEquals(orderPrice, savedItem.getOrderPrice());
        assertEquals(quantity * orderPrice, savedItem.totalPrice());
        assertNotNull(savedItem.getCreatedAt());
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void 주문아이템_정상_조회() {
        // given
        Long orderItemId = 1L;
        Order order = Order.create(1L, List.of(new OrderItemCommand(999L, 1, 1)));
        OrderItem orderItem = OrderItem.create(order, 10L, 2, 15000);
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));

        // when
        OrderItem foundItem = orderItemService.getOrderItemById(orderItemId);

        // then
        assertNotNull(foundItem);
        assertEquals(orderItem.getProductId(), foundItem.getProductId());
        verify(orderItemRepository).findById(orderItemId);
    }

    @Test
    void 주문_조회_실패_테스트() {
        Long orderItemId = 1L;
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> orderItemService.getOrderItemById(orderItemId));
        assertEquals("주문 항목을 찾을 수 없습니다.", e.getMessage());
        verify(orderItemRepository).findById(orderItemId);
    }

    @Test
    void 주문으로_주문아이템_정상_조회() {
        Order order = Order.create(1L, List.of(new OrderItemCommand(999L, 1, 1)));
        OrderItem item1 = OrderItem.create(order, 10L, 2, 15000);
        OrderItem item2 = OrderItem.create(order, 20L, 1, 20000);
        List<OrderItem> items = List.of(item1, item2);
        when(orderItemRepository.findAllByOrder(order)).thenReturn(items);

        List<OrderItem> result = orderItemService.getOrderItemsByOrder(order);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderItemRepository).findAllByOrder(order);
    }

//    @Test
//    void 상위상품_조회() {
//        PopularProductCommand dto1 = new PopularProductCommand(101L, 20);
//        PopularProductCommand dto2 = new PopularProductCommand(102L, 18);
//        PopularProductCommand dto3 = new PopularProductCommand(103L, 15);
//        PopularProductCommand dto4 = new PopularProductCommand(104L, 12);
//        PopularProductCommand dto5 = new PopularProductCommand(105L, 10);
//        PopularProductCommand dto6 = new PopularProductCommand(106L, 8);
//        List<PopularProductCommand> stubList = Arrays.asList(dto1, dto2, dto3, dto4, dto5, dto6);
//
//        when(orderItemRepository.findTopSellingProductDTOs(any(LocalDateTime.class)))
//                .thenReturn(stubList);
//
//        List<PopularProductCommand> result = orderItemService.getPopularProduct();
//
//        assertEquals(5, result.size());
//        assertEquals(dto1.getProductId(), result.get(0).getProductId());
//        verify(orderItemRepository).findTopSellingProductDTOs(any(LocalDateTime.class));
//    }
}
