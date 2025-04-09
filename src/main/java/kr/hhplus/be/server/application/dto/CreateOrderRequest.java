package kr.hhplus.be.server.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
public class CreateOrderRequest {
    private Long userId;
    private List<OrderItemRequest> orderItemRequests;
}
