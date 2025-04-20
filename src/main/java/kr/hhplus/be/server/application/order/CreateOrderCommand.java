package kr.hhplus.be.server.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {
    private Long userId;
    private List<OrderItemCommand> orderItemCommands;
}
