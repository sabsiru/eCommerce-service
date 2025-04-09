package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.application.dto.PopularProductRequest;
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
import static org.mockito.ArgumentMatchers.any;
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
        Long orderId = 1L;
        Long productId = 10L;
        int quantity = 2;
        int orderPrice = 15000;
        OrderItem orderItem = OrderItem.create(orderId, productId, quantity, orderPrice);

        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);

        // when
        OrderItem savedItem = orderItemService.createOrderItem(orderId, productId, quantity, orderPrice);

        // then
        assertNotNull(savedItem);
        assertEquals(orderId, savedItem.orderId());
        assertEquals(productId, savedItem.productId());
        assertEquals(quantity, savedItem.quantity());
        assertEquals(orderPrice, savedItem.orderPrice());
        assertEquals(quantity * orderPrice, savedItem.totalPrice());
        assertNotNull(savedItem.createdAt());
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    void 주문아이템_정상_조회() {
        // given
        Long orderItemId = 1L;
        OrderItem orderItem = OrderItem.create(1L, 10L, 2, 15000);
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));

        // when
        OrderItem foundItem = orderItemService.getOrderItemById(orderItemId);

        // then
        assertNotNull(foundItem);
        assertEquals(orderItem.orderId(), foundItem.orderId());
        verify(orderItemRepository, times(1)).findById(orderItemId);
    }

    @Test
    void 주문_조회_실패_테스트() {
        // given
        Long orderItemId = 1L;
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> orderItemService.getOrderItemById(orderItemId));
        assertEquals("주문 항목을 찾을 수 없습니다.", e.getMessage());
        verify(orderItemRepository, times(1)).findById(orderItemId);
    }

    @Test
    void 주문아이디로_주문아이템_정상_조회() {
        // given
        Long orderId = 1L;
        OrderItem item1 = OrderItem.create(orderId, 10L, 2, 15000);
        OrderItem item2 = OrderItem.create(orderId, 20L, 1, 20000);
        List<OrderItem> items = Arrays.asList(item1, item2);
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(items);

        // when
        List<OrderItem> result = orderItemService.getOrderItemsByOrderId(orderId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderItemRepository, times(1)).findAllByOrderId(orderId);
    }

    @Test
    void 모든_주문아이템_정상_조회() {
        // given
        OrderItem item1 = OrderItem.create(1L, 10L, 2, 15000);
        OrderItem item2 = OrderItem.create(1L, 20L, 1, 20000);
        List<OrderItem> allItems = Arrays.asList(item1, item2);
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(allItems);

        // when
        List<OrderItem> result = orderItemService.getOrderItemsByOrderId(1L);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderItemRepository, times(1)).findAllByOrderId(1L);
    }

    @Test
    public void 상위상품_조회() {
        // Arrange
        // Stub으로 최근 3일간 주문 항목 집계 결과를 반환할 DTO 목록 생성 (총 6건, 상위 5개만 취함)
        PopularProductRequest dto1 = new PopularProductRequest(101L, 20L);
        PopularProductRequest dto2 = new PopularProductRequest(102L, 18L);
        PopularProductRequest dto3 = new PopularProductRequest(103L, 15L);
        PopularProductRequest dto4 = new PopularProductRequest(104L, 12L);
        PopularProductRequest dto5 = new PopularProductRequest(105L, 10L);
        PopularProductRequest dto6 = new PopularProductRequest(106L, 8L);
        List<PopularProductRequest> stubList = Arrays.asList(dto1, dto2, dto3, dto4, dto5, dto6);

        // Repository 메서드 호출 시, stubList를 반환하도록 설정
        when(orderItemRepository.findTopSellingProductDTOs(any(LocalDateTime.class)))
                .thenReturn(stubList);

        // Act
        List<PopularProductRequest> result = orderItemService.getTopSellingProducts();

        // Assert
        // 결과의 크기가 5개여야 함
        assertEquals(5, result.size(), "상위 5개 상품만 반환되어야 합니다.");
        // 옵션: 반환된 리스트의 첫 번째 항목의 productId가 dto1의 productId와 일치하는지 확인 (순서 검증)
        assertEquals(dto1.getProductId(), result.get(0).getProductId(), "첫 번째 상품 ID가 예상과 일치해야 합니다.");

        // Repository 메서드가 정확히 한번 호출되었는지 검증
        verify(orderItemRepository, times(1)).findTopSellingProductDTOs(any(LocalDateTime.class));
    }
}