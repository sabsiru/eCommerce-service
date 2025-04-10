package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.application.order.CreateOrderRequest;
import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.application.order.PopularProductRequest;
import kr.hhplus.be.server.domain.order.Order;
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
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponse orderResponse = orderFacade.processOrder(
                request.getUserId(),
                request.getOrderItemRequests()
        );
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long userId) {
        List<Order> orders = orderFacade.getOrdersByUser(userId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long orderId) {
        Order canceledOrder = orderFacade.cancelOrder(orderId);
        return ResponseEntity.ok(canceledOrder);
    }

    @GetMapping("/popular-products")
    public ResponseEntity<List<PopularProductRequest>> getPopularProducts() {
        List<PopularProductRequest> popularProducts = orderFacade.getPopularProduct();
        return ResponseEntity.ok(popularProducts);
    }

}