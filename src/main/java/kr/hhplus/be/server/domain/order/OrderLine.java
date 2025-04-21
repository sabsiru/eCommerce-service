package kr.hhplus.be.server.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderLine {
    private final Long productId;
    private final int quantity;
    private final int orderPrice;
}