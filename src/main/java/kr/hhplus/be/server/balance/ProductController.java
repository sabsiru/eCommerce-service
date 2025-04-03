package kr.hhplus.be.server.balance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@Tag(name = "Product", description = "상품 관련 API")
public class ProductController {

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "전체 상품 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> listProducts() {
        List<Map<String, Object>> products = List.of(
                Map.of(
                        "productId", 1,
                        "name", "고양이 사료",
                        "price", 15000,
                        "stock", 30
                ),
                Map.of(
                        "productId", 2,
                        "name", "강아지 간식",
                        "price", 5000,
                        "stock", 100
                )
        );

        return ResponseEntity.ok(Map.of(
                "data", products,
                "message", "상품 목록 조회 성공"
        ));
    }


    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회", description = "상품 ID를 기반으로 상세 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getProduct(
            @Parameter(description = "상품 ID", example = "1")
            @PathVariable Long productId
    ) {
        Map<String, Object> product = Map.of(
                "productId", productId,
                "name", "고양이 사료",
                "price", 15000,
                "stock", 30,
                "categoryId", 2,
                "categoryName", "애완동물 용품"
        );

        return ResponseEntity.ok(Map.of(
                "data", product,
                "message", "상품 상세 조회 성공"
        ));
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 상품 조회", description = "최근 3일간 가장 많이 팔린 상위 5개 상품을 조회합니다.")
    public ResponseEntity<List<Map<String, Object>>> getPopularProducts() {
        List<Map<String, Object>> popularProducts = List.of(
                Map.of("productId", 1, "name", "고양이 사료", "price", 15000, "totalSold", 120),
                Map.of("productId", 2, "name", "강아지 간식", "price", 5000, "totalSold", 100),
                Map.of("productId", 3, "name", "햄스터 집", "price", 8000, "totalSold", 80),
                Map.of("productId", 4, "name", "고양이 장난감", "price", 6000, "totalSold", 70),
                Map.of("productId", 5, "name", "강아지 침대", "price", 25000, "totalSold", 60)
        );
        return ResponseEntity.ok(popularProducts);
    }
}
