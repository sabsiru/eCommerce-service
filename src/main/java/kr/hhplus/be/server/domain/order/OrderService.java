package kr.hhplus.be.server.domain.order;

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

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order pay(Long orderId) {
        Order order = getOrderOrThrowPaid(orderId);
        order.pay();
        return save(order);
    }

    public Order cancel(Long orderId) {
        Order order = getOrderOrThrowCancel(orderId);
        order.cancel();
        return save(order);
    }

    public Order updateOrderItems(Long orderId, List<OrderItem> newItems) {
        Order order = getOrderOrThrowCancel(orderId);
        order.updateItems(newItems);
        return save(order);
    }



    public Order save(Order order) {
        Order saved = orderRepository.save(order);
        orderItemRepository.saveAll(order.getItems());
        return saved;
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public List<Order> getOrdersByUser(Long userId) {
        List<Order> byUserId = orderRepository.findByUserId(userId);
        if (byUserId.isEmpty()) {
            throw new IllegalArgumentException("해당 유저가 없거나 주문 목록이 없습니다.");
        }
        return byUserId;
    }

    public Order getOrderOrThrowCancel(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() ->
                new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
        if(order.getStatus() == OrderStatus.CANCEL){
            throw new IllegalStateException(
                    "취소된 주문입니다. orderId=" + orderId);
        }
        return order;
    }

    public Order getOrderOrThrowPaid(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() ->
                new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
        if(order.getStatus() == OrderStatus.PAID){
            throw new IllegalStateException(
                    "결제된 주문입니다. orderId=" + orderId);
        }
        return order;
    }


}