package kr.hhplus.be.server.domain.order;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(Long orderId);

    List<Order> findByUserId(Long userId);

    List<Order> findAll();
}
