package kr.hhplus.be.server.infrastructure.order;

import kr.hhplus.be.server.domain.order.OrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemQueryRepository {
    List<PopularProductRow> findPopularProducts();

    //사용자의 주문과 주문아이템 조회 (아직 사용은 안함)
    List<OrderItem> findOrderItemsByUserId(Long orderId);
}