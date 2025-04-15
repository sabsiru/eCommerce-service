package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(CreateOrderCommand command) {
        return orderRepository.save(
                Order.create(command.getUserId(), command.getOrderItemCommands())
        );
    }

    public Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order payOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.pay();  // 내부 상태 변경
        return order;
    }

    public Order cancelOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.cancel();  // 내부 상태 변경
        return order;
    }

    public Order updateOrderItems(Long orderId, List<OrderItem> newItems) {
        Order order = getOrderOrThrow(orderId);
        order.updateItems(newItems);  // 내부 리스트 갱신 및 총합 재계산
        return order;
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }
}