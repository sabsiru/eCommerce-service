package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.dto.OrderItemRequest;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
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
    public Order processOrder(Long userId,
                              List<OrderItemRequest> orderItemRequests) {
        // 1. 각 주문 항목의 재고 확인 (재고를 가져와 주문 수량과 비교)
        for (OrderItemRequest param : orderItemRequests) {
            int availableStock = productService.getStock(param.getProductId());
            if (availableStock < param.getQuantity()) {
                throw new IllegalStateException("상품 재고가 부족합니다. productId=" + param.getProductId());
            }
        }

        // 2. 빈 주문 항목 리스트 주문 생성하여 order.id 획득
        Order order = orderService.createOrder(userId, new ArrayList<>());
        // DB 저장 후 order.id()는 실제 값(예: 1L)이 할당되어 있다고 가정

        // 3. 생성된 주문의 ID를 사용해 각 주문 항목 생성 및 저장
        List<OrderItem> createdItems = new ArrayList<>();
        for (OrderItemRequest param : orderItemRequests) {
            OrderItem item = orderItemService.createOrderItem(order.id(), param.getProductId(), param.getQuantity(), param.getOrderPrice());
            createdItems.add(item);
        }

        // 4. 생성된 주문 항목들을 주문에 업데이트 (업데이트 메서드는 도메인 요구사항에 따라 구현)
        order = orderService.updateOrderItems(order.id(), createdItems);

        return order;
    }

    //메서드 재사용
    public List<Order> getOrdersByUser(Long userId) {
        return orderService.getOrdersByUser(userId);
    }


}