package kr.hhplus.be.server.infrastructure.order;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemQueryRepository {
    List<PopularProductRow> findPopularProducts();
}