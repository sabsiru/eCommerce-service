package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderCommand request) {
        OrderResponse orderResponse = orderFacade.processOrder(request);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable Long userId) {
        List<Order> orders = orderFacade.getOrdersByUser(userId);
        List<OrderResponse> responseList = orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderFacade.getOrderItems(order.getId()); // facade 통해 조회
                    return OrderResponse.from(order, items);
                })
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long orderId) {
        Order canceledOrder = orderFacade.cancelOrder(orderId);
        List<OrderItem> items = orderFacade.getOrderItems(orderId);
        return ResponseEntity.ok(OrderResponse.from(canceledOrder, items));
    }

}