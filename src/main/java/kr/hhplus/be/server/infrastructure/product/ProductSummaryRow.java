package kr.hhplus.be.server.infrastructure.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProductSummaryRow {
    private Long id;
    private String name;
    private int price;
    //private LocalDateTime createAt; //cursor에서 쓰임
}