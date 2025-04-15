package kr.hhplus.be.server.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {
    private Long userId;
    private List<OrderItemCommand> orderItemCommands;
}
