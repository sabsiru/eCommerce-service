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


    public Order createOrder(Long userId, List<OrderItem> items) {
        Order order = Order.create(userId, items);
        return orderRepository.save(order);
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
        Order updatedOrder = order.pay();
        return orderRepository.save(updatedOrder);
    }

    public Order cancelOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        Order updatedOrder = order.cancel();
        return orderRepository.save(updatedOrder);
    }


    public Order updateOrderItems(Long orderId, List<OrderItem> newItems) {
        // 해당 주문을 조회
        Order order = getOrderOrThrow(orderId);
        // 주문 항목 목록 업데이트를 통해 새로운 Order 객체 생성
        Order updatedOrder = order.updateItems(newItems);
        // 새로운 Order 객체를 Repository에 저장하여 반영한 후 반환

        return orderRepository.save(updatedOrder);
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}