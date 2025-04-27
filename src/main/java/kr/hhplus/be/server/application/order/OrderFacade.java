package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.*;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.domain.user.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserPointService userPointService;


    public OrderResult.Create processOrder(OrderCommand.Create command) {
        userPointService.getUserOrThrow(command.getUserId());
        for (OrderCommand.Item item : command.getItems()) {
            productService.checkStock(item.getProductId(), item.getQuantity());
        }

        List<OrderLine> lines = command.getItems().stream()
                .map(i -> new OrderLine(i.getProductId(), i.getQuantity(), i.getItemPrice()))
                .collect(Collectors.toList());

        Order saved = orderService.create(command.getUserId(), lines);

        List<OrderResult.Item> resultItems = saved.getItems().stream()
                .map(i -> new OrderResult.Item(i.getProductId(), i.getQuantity(), i.getOrderPrice()))
                .collect(Collectors.toList());

        return new OrderResult.Create(
                saved.getId(),
                saved.getUserId(),
                resultItems,
                saved.getTotalAmount(),
                saved.getStatus()
        );
    }

    public OrderResult.Create cancelOrder(Long orderId) {
        Order canceled = orderService.cancel(orderId);

        List<OrderResult.Item> items = OrderMapper.toResultItems(canceled.getItems());

        int totalAmount = items.stream()
                .mapToInt(i -> i.getQuantity() * i.getItemPrice())
                .sum();

        return new OrderResult.Create(
                canceled.getId(),
                canceled.getUserId(),
                items,
                totalAmount,
                canceled.getStatus()
        );
    }

    public List<OrderResult.Create> getOrdersByUser(Long userId) {
        List<Order> orders = orderService.getOrdersByUser(userId);
        return orders.stream()
                .map(order -> {
                    List<OrderResult.Item> items = order.getItems().stream()
                            .map(i -> new OrderResult.Item(i.getProductId(), i.getQuantity(), i.getOrderPrice()))
                            .collect(Collectors.toList());
                    return new OrderResult.Create(
                            order.getId(),
                            order.getUserId(),
                            items,
                            order.getTotalAmount(),
                            order.getStatus()
                    );
                })
                .collect(Collectors.toList());
    }

}
