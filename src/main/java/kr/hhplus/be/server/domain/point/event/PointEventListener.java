package kr.hhplus.be.server.domain.point.event;

import kr.hhplus.be.server.application.user.UserPointFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PointEventListener {
    private final UserPointFacade userPointFacade;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePointUse(PointUseEvent event) {
       userPointFacade.usePoint(event.getUserId(), event.getAmount());
    }
}
