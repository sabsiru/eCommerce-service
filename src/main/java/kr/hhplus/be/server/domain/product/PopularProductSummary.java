package kr.hhplus.be.server.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "popular_product_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularProductSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private int totalQuantity;

    private LocalDate summaryDate;

    public PopularProductSummary(Long productId, int totalQuantity, LocalDate summaryDate) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.summaryDate = summaryDate;
    }
}