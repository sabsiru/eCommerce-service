package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItem createOrderItem(Long orderId, Long productId, int quantity, int orderPrice) {
        OrderItem orderItem = OrderItem.create(orderId, productId, quantity, orderPrice);
        return orderItemRepository.save(orderItem);
    }

    public OrderItem getOrderItemById(Long orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("주문 항목을 찾을 수 없습니다."));
    }

    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId);
    }

    public List<PopularProductRequest> getPopularProduct() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        // Repository의 커스텀 쿼리 메서드를 호출 (DB 연결 없이 테스트에서는 stub 처리됨)
        List<PopularProductRequest> result = orderItemRepository.findTopSellingProductDTOs(threeDaysAgo);
        // 상위 5개만 추출
        return result.stream().limit(5).collect(Collectors.toList());
    }

}