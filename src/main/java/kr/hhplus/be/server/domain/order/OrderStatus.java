package kr.hhplus.be.server.domain.order;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum OrderStatus {
    PENDING, PAID, CANCEL
}