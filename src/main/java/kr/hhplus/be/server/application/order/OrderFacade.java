package kr.hhplus.be.server.application.order;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.order.*;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.domain.user.UserPointService;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserPointService userPointService;

    /**
     * 주문 생성 및 재고 차감 처리
     */
    public OrderResponse processOrder(CreateOrderCommand command) {
        userPointService.getUserOrThrow(command.getUserId());
        // 1. 재고 확인 먼저
        for (OrderItemCommand item : command.getOrderItemCommands()) {
            productService.checkStock(item.getProductId(), item.getQuantity());
        }

        // 2. 주문 도메인에서 order + item 생성 (연관관계도 도메인 내부에서 구성)
        Order order = Order.create(command.getUserId(), command.getOrderItemCommands());

        // 3. 저장
        Order saved = orderService.save(order);

        // 4. 응답 변환
        List<OrderItem> items = order.getItems();
        return OrderResponse.from(saved, items);
    }

    public Order cancelOrder(Long orderId) {
        Order cancelledOrder = orderService.cancel(orderId);

        return cancelledOrder;
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderService.getOrdersByUser(userId);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderService.getOrderItems(orderId);
    }
}
