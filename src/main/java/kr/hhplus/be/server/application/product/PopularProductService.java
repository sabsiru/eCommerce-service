package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.infrastructure.order.OrderItemQueryRepository;
import kr.hhplus.be.server.infrastructure.order.PopularProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PopularProductService {

    private final OrderItemQueryRepository orderItemQueryRepository;

    public List<PopularProductInfo> getPopularProducts() {
        List<PopularProductRow> rows = orderItemQueryRepository.findPopularProducts();
        return rows.stream()
                .map(row -> new PopularProductInfo(row.getProductId(), row.getTotalQuantity()))
                .toList();
    }
}
