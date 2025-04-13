package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.UserCouponStatus;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCouponServiceTest {

    @InjectMocks
    private UserCouponService userCouponService;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("사용자 쿠폰 저장 - 성공")
    void 쿠폰_저장_성공() {
        // given
        UserCoupon userCoupon = new UserCoupon(1L, 1L, 1L, UserCouponStatus.ISSUED, LocalDateTime.now(), null);
        when(userCouponRepository.save(userCoupon)).thenReturn(userCoupon);

        // when
        UserCoupon saved = userCouponService.save(userCoupon);

        // then
        assertEquals(userCoupon, saved);
        verify(userCouponRepository, times(1)).save(userCoupon);
    }

    @Test
    void 단건_조회_성공_테스트() {
        // given
        Long userCouponId = 1L;
        UserCoupon coupon = new UserCoupon(userCouponId, 100L, 500L, UserCouponStatus.ISSUED, LocalDateTime.now().minusDays(5), null);
        when(userCouponRepository.findById(userCouponId)).thenReturn(Optional.of(coupon));

        // when
        UserCoupon result = userCouponService.getById(userCouponId);

        // then
        assertNotNull(result);
        assertEquals(userCouponId, result.id());
        verify(userCouponRepository, times(1)).findById(userCouponId);
    }

    @Test
    void 단건_조회_실패_테스트() {
        // given
        Long userCouponId = 1L;
        when(userCouponRepository.findById(userCouponId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userCouponService.getById(userCouponId)
        );
        assertTrue(exception.getMessage().contains("사용자 쿠폰을 찾을 수 없습니다."));
        verify(userCouponRepository, times(1)).findById(userCouponId);
    }

    @Test
    void 사용자ID와_쿠폰ID_중복_검증_테스트() {
        // given
        Long userId = 100L;
        Long couponId = 500L;
        UserCoupon existing = new UserCoupon(
                1L,
                userId,
                couponId,
                UserCouponStatus.ISSUED,
                LocalDateTime.now().minusDays(5),
                null
        );
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(existing));

        // when & then: validateNotDuplicated()를 호출하면 예외가 발생해야 합니다.
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userCouponService.validateNotDuplicated(userId, couponId)
        );
        assertEquals("이미 발급받은 쿠폰입니다.", exception.getMessage());

        verify(userCouponRepository, times(1)).findByUserIdAndCouponId(userId, couponId);
    }

    @Test
    void useCoupon_성공_테스트() {
        // given
        Long userCouponId = 1L;
        Long userId = 100L;
        Long couponId = 500L;
        LocalDateTime issuedAt = LocalDateTime.now().minusDays(5);
        // ISSUED 상태인 쿠폰 생성
        UserCoupon coupon = new UserCoupon(userCouponId, userId, couponId, UserCouponStatus.ISSUED, issuedAt, null);
        // use() 호출 후 USED 상태로 전환 (사용 시각은 현재 시간)
        UserCoupon usedCoupon = new UserCoupon(userCouponId, userId, couponId, UserCouponStatus.USED, issuedAt, LocalDateTime.now());

        when(userCouponRepository.findById(userCouponId)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(usedCoupon);

        // when
        UserCoupon result = userCouponService.useCoupon(userCouponId);

        // then
        assertNotNull(result);
        assertEquals(UserCouponStatus.USED, result.status());
        assertNotNull(result.usedAt());
        verify(userCouponRepository, times(1)).findById(userCouponId);
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    void refundCoupon_성공_테스트() {
        // given
        Long userCouponId = 1L;
        Long userId = 100L;
        Long couponId = 500L;
        LocalDateTime issuedAt = LocalDateTime.now().minusDays(10);
        LocalDateTime usedAt = LocalDateTime.now().minusDays(1);
        // USED 상태의 쿠폰 생성
        UserCoupon usedCoupon = new UserCoupon(userCouponId, userId, couponId, UserCouponStatus.USED, issuedAt, usedAt);
        // refund() 호출 후 상태가 ISSUED로 변환되고 usedAt은 null 처리됨.
        UserCoupon refundedCoupon = new UserCoupon(userCouponId, userId, couponId, UserCouponStatus.ISSUED, issuedAt, null);

        when(userCouponRepository.findById(userCouponId)).thenReturn(Optional.of(usedCoupon));
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(refundedCoupon);

        // when
        UserCoupon result = userCouponService.refundCoupon(userCouponId);

        // then
        assertNotNull(result);
        assertEquals(UserCouponStatus.ISSUED, result.status());
        assertNull(result.usedAt());
        verify(userCouponRepository, times(1)).findById(userCouponId);
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    void refundCoupon_실패_테스트_사용상태아님() {
        // given
        Long userCouponId = 1L;
        Long userId = 100L;
        Long couponId = 500L;
        LocalDateTime issuedAt = LocalDateTime.now().minusDays(5);
        // ISSUED 상태이면 refund() 호출 시 예외 발생해야 함.
        UserCoupon issuedCoupon = new UserCoupon(userCouponId, userId, couponId, UserCouponStatus.ISSUED, issuedAt, null);

        when(userCouponRepository.findById(userCouponId)).thenReturn(Optional.of(issuedCoupon));

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userCouponService.refundCoupon(userCouponId)
        );
        assertEquals("환불 가능한 쿠폰이 아닙니다.", exception.getMessage());
        verify(userCouponRepository, times(1)).findById(userCouponId);
        verify(userCouponRepository, never()).save(any(UserCoupon.class));
    }
}