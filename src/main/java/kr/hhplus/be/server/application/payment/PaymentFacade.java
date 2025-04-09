package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserPointFacade userPointFacade;
    private final PaymentService paymentService;

    /**
     * 결제 프로세스:
     * 1. 주문 조회: 주문 정보를 OrderService.getOrderOrThrow()로 조회한다.
     * 2. 상품 재고 차감: 각 주문 항목에 대해 productService.decreaseStock()를 호출하여 재고 차감 및 내부 검증을 진행한다.
     * 3. 사용자 포인트 차감: userPointFacade.usePoint()를 호출하여 포인트 차감 및 검증을 처리한다.
     * 4. 주문 상태 업데이트: OrderService.payOrder()를 호출해 주문 상태를 PAID로 전환한다.
     * 5. 결제 상태 업데이트: PaymentService를 사용하여 Payment 객체를 생성(initiating)한 후, completePayment()를 호출해
     *    Payment의 상태를 COMPLETED로 전환한다.
     *
     * @param orderId       주문 ID
     * @param paymentAmount 결제 금액
     * @param usePointAmount 차감할 사용자 포인트
     * @return 최종적으로 결제 완료된 Payment 객체
     */
    public Payment processPayment(Long orderId, int paymentAmount, int usePointAmount) {
        // 1. 주문 조회
        Order order = orderService.getOrderOrThrow(orderId);

        // 2. 상품 재고 차감: 각 주문 항목에 대해 decreaseStock() 호출
        List<?> items = order.items();
        for (Object obj : items) {
            OrderItem item = (OrderItem) obj;
            productService.decreaseStock(item.productId(), item.quantity());
        }

        // 3. 사용자 포인트 차감: userPointFacade.usePoint()에서 포인트 검증 및 차감 처리
        userPointFacade.usePoint(order.userId(), usePointAmount);

        // 4. 주문 상태 업데이트: 주문을 PAID 상태로 전환
        order = orderService.payOrder(orderId);

        // 5. 결제 상태 업데이트: Payment 초기화 후, completePayment()를 통해 Payment 상태를 COMPLETED로 전환
        Payment payment = paymentService.initiatePayment(orderId, paymentAmount);
        payment = paymentService.completePayment(payment.id());

        return payment;
    }
}