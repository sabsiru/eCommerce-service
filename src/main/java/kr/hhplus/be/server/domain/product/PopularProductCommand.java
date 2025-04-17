package kr.hhplus.be.server.domain.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PopularProductCommand {
    private Long productId;
    private int totalQuantity;
}