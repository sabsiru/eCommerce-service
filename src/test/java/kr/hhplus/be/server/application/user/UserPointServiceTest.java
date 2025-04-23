package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserPointService;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPointServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserPointService userPointService;

    private static final LocalDateTime NOW = LocalDateTime.now();




    @Test
    public void 포인트_충전_금액이_0이하일때_예외() {
        // given
        long userId = 1L;
        int initialPoint = 1000;
        int chargeAmount = -500;

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        // when, then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            userPointService.chargePoint(userId, chargeAmount);
        });

        // then
        assertEquals("충전 금액은 0보다 커야 합니다.", e.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void 포인트_최대_잔고_초과시_예외() {
        //given
        long userId = 1L;
        int initialPoint = 9_500_000;
        int chargeAmount = 500_001;

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        ;

        //when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            userPointService.chargePoint(userId, chargeAmount);
        });

        //then
        assertEquals("최대 충전 한도는 `10,000,000원` 입니다.", e.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void 포인트_충전_1회_한도_초과시_에외() {
        //given
        long userId = 1L;
        int initialPoint = 1_000_000;
        int chargeAmount = 1_000_001;

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        ;

        //when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            userPointService.chargePoint(userId, chargeAmount);
        });

        //then
        assertEquals("1회 충전 금액은 `1,000,000원` 입니다.", e.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void 포인트_사용_성공() throws Exception {
        //given
        long userId = 1L;
        int initialPoint = 1_000_000;
        int useAmount = 500_000;
        LocalDateTime now = LocalDateTime.now();

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        //when
        User updatedUser = userPointService.usePoint(userId, useAmount);  // 포인트 사용

        // then
        assertEquals(initialPoint - useAmount, updatedUser.getPoint());  // 포인트가 500_000L이 되어야 함
        verify(userRepository, times(1)).findByIdForUpdate(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void 포인트_사용_금액이_0이하일시_예외() throws Exception {
        //given
        long userId = 1L;
        int initialPoint = 1_000_000;
        int useAmount = 0;

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        ;

        //when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            userPointService.usePoint(userId, useAmount);
        });

        //then
        assertEquals("사용 금액은 0보다 커야 합니다.", e.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void 포인트_사용시_잔액이_부족할시_예외() throws Exception {
        //given
        long userId = 1L;
        int initialPoint = 1_000_000;
        int useAmount = 1_000_001;

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        ;

        //when
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            userPointService.usePoint(userId, useAmount);
        });

        //then
        assertEquals("포인트가 부족합니다.", e.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void 포인트_환불_성공() throws Exception {
        //given
        long userId = 1L;
        int initialPoint = 1_000_000;
        int refundAmount = 1_000_000;

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        //when(userRepository.save(any(User.class))).thenReturn(user);

        //when
        User updatedUser = userPointService.refundPoint(userId, refundAmount);  // 포인트 환불

        // then
        assertEquals(initialPoint + refundAmount, updatedUser.getPoint());  // 포인트가 2_000_000L이 되어야 함
        verify(userRepository, times(1)).findByIdForUpdate(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void 포인트_환불시_금액이_0이하일때_예외() throws Exception {
        //given
        long userId = 1L;
        int initialPoint = 1_000_000;
        int refundAmount = 0;

        User user = new User(userId, "tester", initialPoint, LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        ;

        //when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            userPointService.refundPoint(userId, refundAmount);
        });

        //then
        assertEquals("환불 금액은 0보다 커야 합니다.", e.getMessage());
        verify(userRepository, never()).save(any(User.class));

    }

    @Test
    void 유저없을때_충전_예외() {
        // given
        long userId = 1L;
        int chargeAmount = 5000;

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> userPointService.chargePoint(userId, chargeAmount));

        assertEquals("유저를 찾을수 없습니다.", e.getMessage());
        verify(userRepository, times(1)).findByIdForUpdate(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 유저없을때_사용_예외() {
        // given
        long userId = 1L;
        int chargeAmount = 5000;

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> userPointService.usePoint(userId, chargeAmount));

        assertEquals("유저를 찾을수 없습니다.", e.getMessage());
        verify(userRepository, times(1)).findByIdForUpdate(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 유저없을때_환불_예외() {
        // given
        long userId = 1L;
        int chargeAmount = 5000;

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> userPointService.refundPoint(userId, chargeAmount));

        assertEquals("유저를 찾을수 없습니다.", e.getMessage());
        verify(userRepository, times(1)).findByIdForUpdate(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 유저없을때_포인트조회_예외() {
        // given
        long userId = 1L;

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> userPointService.getPoint(userId));

        assertEquals("유저를 찾을수 없습니다.", e.getMessage());
        verify(userRepository, times(1)).findByIdForUpdate(userId);
        verify(userRepository, never()).save(any(User.class));
    }

}