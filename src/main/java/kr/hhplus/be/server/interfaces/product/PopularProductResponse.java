package kr.hhplus.be.server.interfaces.product;

import lombok.Getter;

@Getter
public class PopularProductResponse {

    private final Long productId;
    private final Long totalQuantity;

    public PopularProductResponse(Long productId, Long totalQuantity) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
    }
}
