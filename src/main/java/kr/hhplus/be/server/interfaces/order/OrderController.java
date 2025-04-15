package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderFacade;
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
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderCommand request) {
        OrderResponse orderResponse = orderFacade.processOrder(request);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable Long userId) {
        List<Order> orders = orderFacade.getOrdersByUser(userId);
        List<OrderResponse> responseList = orders.stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        Order canceledOrder = orderFacade.cancelOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(canceledOrder));
    }

//    @GetMapping("/popular-products")
//    public ResponseEntity<List<PopularProductCommand>> getPopularProducts() {
//        List<PopularProductCommand> popularProducts = orderFacade.getPopularProduct();
//        return ResponseEntity.ok(popularProducts);
//    }

}