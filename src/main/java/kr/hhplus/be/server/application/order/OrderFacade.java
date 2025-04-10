package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderFacade {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ProductService productService;

    /**
     * 재고 확인과 주문 생성(및 주문 항목 저장) 프로세스.
     * 각 주문 항목별로, ProductService.getStock(productId)를 통해 현재 재고를 조회한 후,
     * 주문 수량보다 재고가 부족하면 예외를 던진다.
     * 충분할 경우 주문 생성 후 OrderItemService를 통해 주문 항목을 생성 및 저장하고,
     * 생성된 주문 항목들을 주문에 업데이트한 최종 주문(Order) 객체를 반환한다.
     *
     * @param userId          주문자 ID
     * @param orderItemRequests 주문 항목 생성을 위한 파라미터 목록
     * @return 최종 생성된 Order 객체
     * @throws IllegalStateException 재고 부족 시 예외 발생
     */
    public OrderResponse processOrder(Long userId, List<OrderItemRequest> orderItemRequests) {
        // 1. 각 주문 항목의 재고 확인
        for (OrderItemRequest param : orderItemRequests) {
            int availableStock = productService.getStock(param.getProductId());
            if (availableStock < param.getQuantity()) {
                throw new IllegalStateException("상품 재고가 부족합니다.");
            }
        }

        // 2. 주문 생성
        Order order = orderService.createOrder(userId, new ArrayList<>());

        // 3. 주문 항목 생성
        List<OrderItem> createdItems = new ArrayList<>();
        for (OrderItemRequest param : orderItemRequests) {
            OrderItem item = orderItemService.createOrderItem(order.id(), param.getProductId(), param.getQuantity(), param.getOrderPrice());
            createdItems.add(item);
        }

        // 4. 주문 항목 업데이트
        order = orderService.updateOrderItems(order.id(), createdItems);

        // 5. DTO로 변환하여 반환
        return OrderResponse.from(order);
    }

    public Order cancelOrder(Long orderId) {
        orderService.getOrderOrThrow(orderId);
        return orderService.cancelOrder(orderId);
    }

    // 메서드 재사용
    public List<Order> getOrdersByUser(Long userId) {
        return orderService.getOrdersByUser(userId);
    }

    // 메서드 재사용
    public List<PopularProductRequest> getPopularProduct() {
        return orderItemService.getPopularProduct();
    }


}