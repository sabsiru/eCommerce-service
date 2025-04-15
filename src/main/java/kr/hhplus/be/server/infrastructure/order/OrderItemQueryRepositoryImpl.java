package kr.hhplus.be.server.infrastructure.order;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.hhplus.be.server.domain.order.QOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderItemQueryRepositoryImpl implements OrderItemQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<PopularProductRow> findPopularProducts() {
        QOrderItem orderItem = QOrderItem.orderItem;

        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusDays(4);

        return queryFactory
                .select(Projections.constructor(PopularProductRow.class,
                        orderItem.productId,
                        orderItem.quantity.sum()))
                .from(orderItem)
                .where(orderItem.createdAt.goe(from)
                        .and(orderItem.createdAt.lt(to)))
                .groupBy(orderItem.productId)
                .orderBy(orderItem.quantity.sum().desc())
                .limit(5)
                .fetch();
    }
}