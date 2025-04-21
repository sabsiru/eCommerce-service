package kr.hhplus.be.server.infrastructure.order;

import kr.hhplus.be.server.domain.order.OrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemQueryRepository {
    List<PopularProductRow> findPopularProducts();

    List<OrderItem> findOrderItemsByUserId(Long orderId);
}