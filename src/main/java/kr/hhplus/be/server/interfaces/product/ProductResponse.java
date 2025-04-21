package kr.hhplus.be.server.interfaces.product;

import kr.hhplus.be.server.infrastructure.product.ProductSummaryRow;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private int price;

    public static ProductResponse from(ProductSummaryRow row) {
        return new ProductResponse(
                row.getId(),
                row.getName(),
                row.getPrice()
        );
    }
}