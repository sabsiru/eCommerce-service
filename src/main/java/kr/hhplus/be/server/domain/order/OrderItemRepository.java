package kr.hhplus.be.server.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findById(Long orderItemId);

    OrderItem save(OrderItem orderItem);

    //List<PopularProductCommand> findTopSellingProductDTOs(LocalDateTime fromDate);

    List<OrderItem> findAllByOrder(Order order);
}
