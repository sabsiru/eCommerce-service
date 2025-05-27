package kr.hhplus.be.server.domain.point.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PointUseEvent {
    private final Long userId;
    private final int amount;

    public PointUseEvent(Long userId, int amount) {
        this.userId = userId;
        this.amount = amount;
    }
}
