package kr.hhplus.be.server.application.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PopularProductInfo {
    private final Long productId;
    private final int totalQuantity;

    @JsonCreator
    public PopularProductInfo(@JsonProperty("productId") Long productId,
                               @JsonProperty("totalQuantity") int totalQuantity) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
    }
}