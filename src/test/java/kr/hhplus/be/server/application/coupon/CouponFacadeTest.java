package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.*;
import kr.hhplus.be.server.domain.order.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @Mock
    private CouponService couponService;

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

        UserCoupon savedUserCoupon = new UserCoupon(
                1L, userId, couponId, UserCouponStatus.ISSUED, now, null
        );

        when(couponService.issueCoupon(userId, couponId))
                .thenReturn(savedUserCoupon);

        // when
        UserCoupon result = couponFacade.issueCoupon(userId, couponId);

        // then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(couponId, result.getCouponId());
        assertEquals(UserCouponStatus.ISSUED, result.getStatus());

        verify(couponService, times(1)).issueCoupon(userId, couponId);
    }

    @Test
    void 쿠폰_중복발급_예외_테스트() {
        // given
        Long userId = 100L;
        Long couponId = 500L;

        doThrow(new IllegalStateException("이미 발급받은 쿠폰입니다."))
                .when(couponService).issueCoupon(userId, couponId);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                couponFacade.issueCoupon(userId, couponId)
        );
        assertEquals("이미 발급받은 쿠폰입니다.", exception.getMessage());

        verify(couponService, times(1)).issueCoupon(userId, couponId);
    }
}