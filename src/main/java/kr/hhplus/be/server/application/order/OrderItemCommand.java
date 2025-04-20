package kr.hhplus.be.server.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCommand {
    private Long productId;
    private int quantity;
    private int orderPrice;
}

