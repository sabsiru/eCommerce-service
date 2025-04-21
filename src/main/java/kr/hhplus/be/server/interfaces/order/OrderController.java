package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.application.order.OrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody OrderRequest request) {
        List<OrderCommand.Item> items = request.getItems().stream()
                .map(i -> new OrderCommand.Item(i.getProductId(), i.getQuantity(),i.getItemPrice()))
                .collect(Collectors.toList());

        OrderCommand.Create command = new OrderCommand.Create(request.getUserId(), items);

        // Facade 호출
        OrderResult.Create result = orderFacade.processOrder(command);

        // Result → Web
        OrderResponse response = OrderResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable Long userId) {
        List<OrderResult.Create> results = orderFacade.getOrdersByUser(userId);

        List<OrderResponse> responseList = results.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long orderId) {
        OrderResult.Create result = orderFacade.cancelOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(result));
    }

}