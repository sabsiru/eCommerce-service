package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.point.PointHistoryService;
import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.PointHistoryType;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserPointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPointFacadeTest {
    @InjectMocks
    private UserPointFacade userPointFacade;  // Facade 객체

    @Mock
    private UserPointService userPointService;

    @Mock
    private PointHistoryService pointHistoryService;

    @Test
    public void 포인트_정상_충전시_충전금액_정상() throws Exception{
        //given
        long userId = 1L;
        int initialPoint = 1000;
        int chargeAmount = 5000;
        int expectedPoint = initialPoint + chargeAmount;

        User user = new User(userId, "tester", expectedPoint, LocalDateTime.now(), LocalDateTime.now());
        PointHistory pointHistory = new PointHistory(null, userId, chargeAmount, PointHistoryType.CHARGE, user.getCreatedAt(), null);

        when(userPointService.chargePoint(userId,chargeAmount)).thenReturn(user);
        when(pointHistoryService.saveCharge(userId,chargeAmount)).thenReturn(pointHistory);

        //when
        User chargedPoint = userPointFacade.chargePoint(userId, chargeAmount);

        //then
        assertEquals(expectedPoint, chargedPoint.getPoint());
        verify(userPointService,times(1)).chargePoint(userId,chargeAmount);
        verify(pointHistoryService,times(1)).saveCharge(userId,chargeAmount);
    }

    @Test
    void 포인트_충전시_충전내역_타입_확인() {
        // given
        long userId = 1L;
        int initialPoint = 1000;
        int chargeAmount = 5000;
        int expectedPoint = initialPoint + chargeAmount;

        // userPointService는 충전 후 업데이트된 User를 반환하도록 설정
        User updatedUser = new User(userId, "tester", expectedPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userPointService.chargePoint(userId, chargeAmount)).thenReturn(updatedUser);

        // saveCharge 메서드가 호출될 때, PointHistory.charge()를 사용하여 생성된 객체를 반환하도록 Answer 설정
        // Answer 내부에서 생성된 객체를 반환하여 이후 검증 가능하게 함.
        when(pointHistoryService.saveCharge(anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    long uid = invocation.getArgument(0);
                    long amt = invocation.getArgument(1);
                    return PointHistory.charge(uid, amt);
                });

        // when
        userPointFacade.chargePoint(userId, chargeAmount);

        // then
        PointHistory expectedHistory = PointHistory.charge(userId, chargeAmount);
        assertEquals(PointHistoryType.CHARGE, expectedHistory.getType());

        verify(userPointService, times(1)).chargePoint(userId, chargeAmount);
        verify(pointHistoryService, times(1)).saveCharge(userId, chargeAmount);
    }

    @Test
    void 포인트_충전시_잘못된_충전금액_입력시_예외전파() {
        // given
        long userId = 1L;
        int chargePoint = 0; // 0 이하이면 예외 발생 조건

        // userPointService가 잘못된 충전 금액에 대해 예외를 던지도록 설정합니다.
        when(userPointService.chargePoint(userId, chargePoint))
                .thenThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다."));

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> userPointFacade.chargePoint(userId, chargePoint));

        assertEquals("충전 금액은 0보다 커야 합니다.", e.getMessage());

        // 예외 발생 시 pointHistoryService.saveCharge가 호출되지 않아야 합니다.
        verify(pointHistoryService, never()).saveCharge(anyLong(), anyLong());
    }

    @Test
    void 포인트_충전시_최대충전한도_초과시_예외전파() {
        // given
        long userId = 1L;
        // 예를 들어, 사용자의 현재 포인트가 9,500,000원인 상황에서 600,000원을 충전하면,
        // 9,500,000 + 600,000 = 10,100,000원이 되어 최대 한도 10,000,000원을 초과하게 됩니다.
        int chargeAmount = 600_000;

        // userPointService의 chargePoint 메서드가 최대 한도 초과 시 IllegalArgumentException을 던지도록 설정
        when(userPointService.chargePoint(userId, chargeAmount))
                .thenThrow(new IllegalArgumentException("최대 충전 한도는 `10,000,000원` 입니다."));

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userPointFacade.chargePoint(userId, chargeAmount));
        assertEquals("최대 충전 한도는 `10,000,000원` 입니다.", ex.getMessage());

        // 예외 발생 시 충전 내역 저장은 진행되지 않아야 함
        verify(userPointService, times(1)).chargePoint(userId, chargeAmount);
        verify(pointHistoryService, never()).saveCharge(anyLong(), anyLong());
    }

    @Test
    public void 포인트_충전시_1회_최대충전한도_초과시_예외전파() throws Exception{
        // given
        long userId = 1L;
        int chargePoint = 1_000_001; // 100만 이상이면 예외 발생 조건

        // userPointService가 잘못된 충전 금액에 대해 예외를 던지도록 설정합니다.
        when(userPointService.chargePoint(userId, chargePoint))
                .thenThrow(new IllegalArgumentException("1회 충전 금액은 `1,000,000원` 입니다."));

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> userPointFacade.chargePoint(userId, chargePoint));

        assertEquals("1회 충전 금액은 `1,000,000원` 입니다.", e.getMessage());

        // 예외 발생 시 pointHistoryService.saveCharge가 호출되지 않아야 합니다.
        verify(pointHistoryService, never()).saveCharge(anyLong(), anyLong());

    }

    @Test
    public void 포인트_정상_사용시_사용금액_정상() throws Exception{
        //given
        long userId = 1L;
        int initialPoint = 6000;
        int useAmount = 5000;
        int expectedPoint = initialPoint - useAmount;

        User user = new User(userId, "tester", expectedPoint, LocalDateTime.now(), LocalDateTime.now());
        PointHistory pointHistory = new PointHistory(null, userId, useAmount, PointHistoryType.USE, user.getCreatedAt(), null);

        when(userPointService.usePoint(userId,useAmount)).thenReturn(user);
        when(pointHistoryService.saveUse(userId,useAmount)).thenReturn(pointHistory);

        //when
        User chargedPoint = userPointFacade.usePoint(userId, useAmount);

        //then
        assertEquals(expectedPoint, chargedPoint.getPoint());
        verify(userPointService,times(1)).usePoint(userId,useAmount);
        verify(pointHistoryService,times(1)).saveUse(userId,useAmount);
    }

    @Test
    void 포인트_사용시_사용내역_타입_확인() {
        // given
        long userId = 1L;
        int useAmount = 200; // 포인트 사용 금액
        int initialPoint = 1000;
        int expectedPoint = initialPoint - useAmount;

        // userPointService가 사용 후 업데이트된 User를 반환하도록 설정
        User updatedUser = new User(userId, "tester", expectedPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userPointService.usePoint(userId, useAmount)).thenReturn(updatedUser);

        // pointHistoryService.saveUse 메서드가 호출될 때,
        // 전달받은 인자를 가지고 PointHistory.use() 정적 팩토리 메서드를 사용해 객체를 생성하여 반환하도록 설정
        when(pointHistoryService.saveUse(userId, useAmount))
                .thenAnswer(invocation -> {
                    long uid = invocation.getArgument(0);
                    long amt = invocation.getArgument(1);
                    return PointHistory.use(uid, amt);
                });

        // when
        userPointFacade.usePoint(userId, useAmount);

        // then
        // 예상되는 PointHistory 객체 생성
        PointHistory expectedHistory = PointHistory.use(userId, useAmount);
        assertEquals(PointHistoryType.USE, expectedHistory.getType());

        verify(userPointService, times(1)).usePoint(userId, useAmount);
        verify(pointHistoryService, times(1)).saveUse(userId, useAmount);
    }

    @Test
    public void 포인트_정상_환불시_환불금액_정상() throws Exception{
        //given
        long userId = 1L;
        int initialPoint = 6000;
        int useAmount = 5000;
        int expectedPoint = initialPoint + useAmount;
        long orderId=1L;

        User user = new User(userId, "tester", expectedPoint, LocalDateTime.now(), LocalDateTime.now());
        PointHistory pointHistory = new PointHistory(null, userId, useAmount, PointHistoryType.REFUND, user.getCreatedAt(), orderId);

        when(userPointService.refundPoint(userId,useAmount)).thenReturn(user);
        when(pointHistoryService.saveRefund(userId,useAmount,orderId)).thenReturn(pointHistory);

        //when
        User refundPoint = userPointFacade.refundPoint(userId, useAmount,orderId);

        //then
        assertEquals(expectedPoint, refundPoint.getPoint());
        verify(userPointService,times(1)).refundPoint(userId,useAmount);
        verify(pointHistoryService,times(1)).saveRefund(userId,useAmount,orderId);
    }

    @Test
    void 포인트_환불시_환불내역_타입_확인() {
        // given
        long userId = 1L;
        int refundAmount = 300; // 환불 금액
        Long orderId = 10L;     // 관련 주문 ID
        int initialPoint = 1000;
        int expectedPoint = initialPoint + refundAmount;

        // userPointService가 환불 후 업데이트된 User를 반환하도록 설정
        User updatedUser = new User(userId, "tester", expectedPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userPointService.refundPoint(userId, refundAmount)).thenReturn(updatedUser);

        // pointHistoryService.saveRefund 메서드가 호출될 때,
        // 전달받은 인자들을 활용해서 PointHistory.refund()를 통해 객체를 생성해 반환하도록 설정
        when(pointHistoryService.saveRefund(userId, refundAmount, orderId))
                .thenAnswer(invocation -> {
                    long uid = invocation.getArgument(0);
                    long amt = invocation.getArgument(1);
                    long order = invocation.getArgument(2);
                    return PointHistory.refund(uid, amt, order);
                });

        // when
        userPointFacade.refundPoint(userId, refundAmount, orderId);

        // then
        // 예상되는 PointHistory 객체 생성
        PointHistory expectedHistory = PointHistory.refund(userId, refundAmount, orderId);
        assertEquals(PointHistoryType.REFUND, expectedHistory.getType());
        assertEquals(orderId, expectedHistory.getRelatedOrderId());

        verify(userPointService, times(1)).refundPoint(userId, refundAmount);
        verify(pointHistoryService, times(1)).saveRefund(userId, refundAmount, orderId);
    }
  
}