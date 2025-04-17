package kr.hhplus.be.server.infrastructure.product;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.QProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ProductSummaryRow> findLatestProducts(int offset, int limit) {
        QProduct product = QProduct.product;

        return queryFactory
                .select(Projections.constructor(ProductSummaryRow.class,
                        product.id,
                        product.name,
                        product.price))
                .from(product)
                .orderBy(product.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public List<ProductSummaryRow> findProductsByCursor(LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        QProduct product = QProduct.product;

        BooleanBuilder condition = new BooleanBuilder();

        if (cursorCreatedAt != null && cursorId != null) {
            condition.and(
                    product.createdAt.lt(cursorCreatedAt)
                            .or(product.createdAt.eq(cursorCreatedAt).and(product.id.lt(cursorId)))
            );
        }

        return queryFactory
                .select(Projections.constructor(ProductSummaryRow.class,
                        product.id,
                        product.name,
                        product.price,
                        product.createdAt
                ))
                .from(product)
                .where(condition)
                .orderBy(product.createdAt.desc(), product.id.desc())
                .limit(limit)
                .fetch();
    }
}

