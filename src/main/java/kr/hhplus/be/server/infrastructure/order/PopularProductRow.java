package kr.hhplus.be.server.infrastructure.order;

import lombok.Getter;

@Getter
public class PopularProductRow {

    private final Long productId;
    private final int totalQuantity;

    public PopularProductRow(Long productId, int totalQuantity) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
    }
}