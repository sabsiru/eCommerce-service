package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.domain.coupon.UserCouponStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @Mock
    private CouponService couponService;

    @Mock
    private UserCouponService userCouponService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CouponFacade couponFacade;

    @Test
    void 쿠폰_정상_발급_테스트() {
        // given
        Long userId = 100L;
        Long couponId = 500L;
        LocalDateTime now = LocalDateTime.now();

        // couponService.issueCoupon() Stub: coupon 객체 반환 (쿠폰의 id는 couponId로 가정)
        Coupon coupon = new Coupon(couponId, "테스트 쿠폰", 20, 2000, CouponStatus.ACTIVE,
                now.plusDays(7), now, 100, 0);
        when(couponService.issueCoupon(couponId)).thenReturn(coupon);

        // userCouponService.validateNotDuplicated()는 예외 없이 통과하도록 설정 (void 메서드이므로 doNothing()은 생략 가능)
        // userCouponService.save() Stub: 저장된 UserCoupon을 반환 (id 부여된 상태)
        UserCoupon savedUserCoupon = new UserCoupon(1L, userId, coupon.getId(), UserCouponStatus.ISSUED, now, null);
        when(userCouponService.save(any(UserCoupon.class))).thenReturn(savedUserCoupon);

        // when
        UserCoupon result = couponFacade.issueCoupon(userId, couponId);

        // then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(couponId, result.getCouponId());
        assertEquals(UserCouponStatus.ISSUED, result.getStatus());
        // validateNotDuplicated가 호출되어 중복체크가 수행됨을 검증 (예외 없으므로 추가 검증은 verify로)
        verify(userCouponService, times(1)).validateNotDuplicated(userId, couponId);
        verify(couponService, times(1)).issueCoupon(couponId);
        verify(userCouponService, times(1)).save(any(UserCoupon.class));
    }

    @Test
    void 쿠폰_중복발급_예외_테스트() {
        // given
        Long userId = 100L;
        Long couponId = 500L;
        // validateNotDuplicated에서 중복 존재 시 IllegalStateException 발생하도록 설정
        doThrow(new IllegalStateException("이미 발급받은 쿠폰입니다."))
                .when(userCouponService).validateNotDuplicated(userId, couponId);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                couponFacade.issueCoupon(userId, couponId)
        );
        assertEquals("이미 발급받은 쿠폰입니다.", exception.getMessage());

        // couponService.issueCoupon과 userCouponService.save는 호출되지 않아야 함
        verify(couponService, never()).issueCoupon(any());
        verify(userCouponService, never()).save(any());
    }
}