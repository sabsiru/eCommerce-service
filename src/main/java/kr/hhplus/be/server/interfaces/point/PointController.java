package kr.hhplus.be.server.interfaces.point;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/point")
@Tag(name = "point", description = "잔액 충전 및 조회 API")
public class PointController {

//    @PostMapping("/{userId}/charge")
//    @Operation(
//            summary = "잔액 충전",
//            description = "사용자의 잔액을 충전합니다. \n\n※ userId는 URL Path로 전달되며, 충전 금액은 Request Body로 전달됩니다."
//    )
//    public ResponseEntity<Map<String, Object>> chargePoint(
//            @Parameter(description = "충전할 사용자 ID", example = "1")
//            @PathVariable Long userId,
//
//            @RequestBody
//            @Parameter(description = "충전 금액 (JSON 형태)", required = true, example = "{\"amount\":5000}")
//            Map<String, Object> body
//    ) {
//        Integer amount = Integer.parseInt(body.get("amount").toString());
//        int updatedBalance = amount;
//
//        return ResponseEntity.ok(Map.of(
//                "userId", userId,
//                "point", updatedBalance,
//                "message", "충전이 완료되었습니다."
//        ));
//    }
//
//    @GetMapping("/{userId}")
//    @Operation(
//            summary = "잔액 조회",
//            description = "사용자의 잔액을 조회합니다."
//    )
//    public ResponseEntity<Map<String, Object>> getPoint(
//            @Parameter(description = "조회할 사용자 ID", example = "1")
//            @PathVariable Long userId
//    ) {
//        int mockBalance = 10000;
//
//        return ResponseEntity.ok(Map.of(
//                "userId", userId,
//                "point", mockBalance,
//                "message", "잔액 조회 성공"
//        ));
//    }
}