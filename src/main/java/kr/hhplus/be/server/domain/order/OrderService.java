package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.application.order.OrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Order create(Long userId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 비어 있습니다.");
        }
        Order order = new Order(userId);
        for (OrderLine line : lines) {
            order.addLine(line.getProductId(), line.getQuantity(), line.getOrderPrice());
        }
        return save(order);
    }

    public Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order pay(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.pay();  // 내부 상태 변경
        return save(order);
    }

    public Order cancel(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        order.cancel();  // 내부 상태 변경
        return save(order);
    }

    public Order updateOrderItems(Long orderId, List<OrderItem> newItems) {
        Order order = getOrderOrThrow(orderId);
        order.updateItems(newItems);  // 내부 리스트 갱신 및 총합 재계산
        return save(order);
    }

    public List<Order> getOrdersByUser(Long userId) {
        List<Order> byUserId = orderRepository.findByUserId(userId);
       if (byUserId.isEmpty()) {
           throw new IllegalArgumentException("해당 유저가 없거나 주문 목록이 없습니다.");
       }
        return byUserId;
    }

    public Order save(Order order) {
        Order saved = orderRepository.save(order);
        orderItemRepository.saveAll(order.getItems());
        return saved;
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }


}