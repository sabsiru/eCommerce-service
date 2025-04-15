package kr.hhplus.be.server.application.product;

import lombok.Getter;

@Getter
public class PopularProductInfo {
    private final Long productId;
    private final int totalQuantity;

    public PopularProductInfo(Long productId, int totalQuantity) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
    }
}