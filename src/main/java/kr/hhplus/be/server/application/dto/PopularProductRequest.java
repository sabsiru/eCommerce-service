package kr.hhplus.be.server.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PopularProductRequest {
    private Long productId;
    private Long totalQuantity;
}