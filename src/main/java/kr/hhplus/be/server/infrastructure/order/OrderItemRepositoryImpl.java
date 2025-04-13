package kr.hhplus.be.server.infrastructure.order;

import kr.hhplus.be.server.application.order.PopularProductRequest;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderItemRepositoryImpl implements OrderItemRepository {
    @Override
    public List<OrderItem> findAllByOrderId(Long orderId) {
        return List.of();
    }

    @Override
    public Optional<OrderItem> findById(Long orderItemId) {
        return Optional.empty();
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        return null;
    }

    @Override
    public List<PopularProductRequest> findTopSellingProductDTOs(LocalDateTime fromDate) {
        return List.of();
    }
}
