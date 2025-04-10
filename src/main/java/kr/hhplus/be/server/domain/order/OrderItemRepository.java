package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.application.order.PopularProductRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository {
    List<OrderItem> findAllByOrderId(Long orderId);

    Optional<OrderItem> findById(Long orderItemId);

    OrderItem save(OrderItem orderItem);

    List<PopularProductRequest> findTopSellingProductDTOs(LocalDateTime fromDate);
}
