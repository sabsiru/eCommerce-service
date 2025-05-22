package kr.hhplus.be.server.domain.point.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishPointUsed(PointUseEvent event) {
        eventPublisher.publishEvent(event);
    }
}
