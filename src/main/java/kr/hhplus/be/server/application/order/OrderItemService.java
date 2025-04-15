package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItem createOrderItem(Order order, Long productId, int quantity, int orderPrice) {
        OrderItem orderItem = OrderItem.create(order, productId, quantity, orderPrice);
        return orderItemRepository.save(orderItem);
    }

    public OrderItem getOrderItemById(Long orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("주문 항목을 찾을 수 없습니다."));
    }

    public List<OrderItem> getOrderItemsByOrder(Order order) {
        return orderItemRepository
                .findAllByOrder(order);
    }

    //querydsl 구현으로 사용 안함
//    public List<PopularProductCommand> getPopularProduct() {
//        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
//        List<PopularProductCommand> result = orderItemRepository.findTopSellingProductDTOs(threeDaysAgo);
//        return result.stream().limit(5).collect(Collectors.toList());
//    }
}
