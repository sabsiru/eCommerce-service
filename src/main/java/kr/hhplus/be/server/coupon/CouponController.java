package kr.hhplus.be.server.coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coupon")
@Tag(name = "Coupon", description = "쿠폰 관련 API")
public class CouponController {

    @GetMapping("/available")
    @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "시스템에서 발급 가능한 쿠폰 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getAvailableCoupons() {
        List<Map<String, Object>> coupons = List.of(
                Map.of(
                        "couponId", 101,
                        "name", "10% 할인 쿠폰",
                        "discountRate", 10,
                        "expiredAt", "2025-04-30"
                ),
                Map.of(
                        "couponId", 102,
                        "name", "5% 할인 쿠폰",
                        "discountRate", 5,
                        "expiredAt", "2025-04-10"
                )
        );

        return ResponseEntity.ok(Map.of(
                "data", coupons,
                "message", "발급 가능한 쿠폰 목록 조회 성공"
        ));
    }

    @PostMapping("/{userId}/issue")
    @Operation(summary = "쿠폰 발급", description = "선착순 쿠폰을 발급합니다.")
    public ResponseEntity<Map<String, Object>> issueCoupon(
            @Parameter(description = "쿠폰을 발급받을 사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        Map<String, Object> coupon = Map.of(
                "couponId", 101,
                "name", "10% 할인 쿠폰",
                "discountRate", 10,
                "expiredAt", "2025-04-30"
        );

        return ResponseEntity.ok(coupon);
    }

    @GetMapping("/{userId}/list")
    @Operation(summary = "보유 쿠폰 목록 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getUserCoupons(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        List<Map<String, Object>> coupons = List.of(
                Map.of(
                        "couponId", 101,
                        "name", "10% 할인 쿠폰",
                        "discountRate", 10,
                        "expiredAt", "2025-04-30",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "couponId", 102,
                        "name", "5% 할인 쿠폰",
                        "discountRate", 5,
                        "expiredAt", "2025-04-01",
                        "status", "EXPIRED"
                )
        );

        return ResponseEntity.ok(Map.of(
                "data", coupons
        ));
    }

    @PostMapping("/{userId}/use")
    @Operation(summary = "쿠폰 사용", description = "사용자가 보유한 쿠폰을 사용합니다.")
    public ResponseEntity<Map<String, Object>> useCoupon(
            @Parameter(description = "쿠폰을 사용할 사용자 ID", example = "1")
            @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용할 쿠폰 ID", required = true
            )
            @RequestBody Map<String, Object> request
    ) {
        Long couponId = Long.valueOf(request.get("couponId").toString());

        Map<String, Object> coupon = Map.of(
                "couponId", couponId,
                "orderId", "1234",
                "name", "10% 할인 쿠폰",
                "discountRate", 10,
                "expiresAt", "2025-04-30",
                "couponStatus", "USED"
        );

        return ResponseEntity.ok(Map.of(
                "data", coupon
        ));
    }
}
