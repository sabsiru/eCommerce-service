package kr.hhplus.be.server.interfaces.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@Tag(name = "주문", description = "주문 관련 API")
public class OrderController {

    @PostMapping("/{userId}/create")
    @Operation(summary = "주문 생성", description = "사용자가 선택한 상품들을 주문으로 생성합니다.")
    public ResponseEntity<Map<String, Object>> createOrder(
            @Parameter(description = "주문을 생성할 사용자 ID", example = "1")
            @PathVariable Long userId,
            @RequestBody HashMap<String, Object> request
    ) {

        List<Map<String, Object>> items = List.of(
                Map.of("productId", 1, "quantity", 2),  // 상품 1, 수량 2
                Map.of("productId", 3, "quantity", 1)   // 상품 3, 수량 1
        );


        Long orderId = 101L;  // 생성된 주문 ID (임시값)
        int totalAmount = items.stream().mapToInt(item -> (int) item.get("quantity") * 5000).sum(); // 예시로 계산


        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("totalAmount", totalAmount);

        return ResponseEntity.ok(Map.of(
                "data", response,
                "message", "주문이 성공적으로 생성되었습니다."
        ));
    }

    @PostMapping("/{userId}/payment")
    @Operation(summary = "결제", description = "사용자가 주문을 결제하고 잔액을 차감합니다.")
    public ResponseEntity<Map<String, Object>> payment(
            @Parameter(description = "결제할 사용자 ID", example = "1")
            @PathVariable Long userId,
            @RequestBody HashMap<String, Object> request
    ) {
        List<Map<String, Object>> items = List.of(
                Map.of("productId", 1, "quantity", 2),  // 상품 1, 수량 2
                Map.of("productId", 3, "quantity", 1)   // 상품 3, 수량 1
        );


        Long orderId = 101L;  // 생성된 주문 ID (임시값)
        int paymentAmount = items.stream().mapToInt(item -> (int) item.get("quantity") * 5000).sum(); // 예시로 계산


        int remainingBalance = 20000; // 예시 잔액
        remainingBalance -= paymentAmount; // 결제 후 잔액 차감

        // 결제 완료 응답
        Map<String, Object> response = new HashMap<>();
        response.put("message", "결제가 완료되었습니다.");
        response.put("orderId", orderId);
        response.put("status", "PAID");
        response.put("paymentAmount", paymentAmount);
        response.put("remainingBalance", remainingBalance);

        return ResponseEntity.ok(response);
    }
}
