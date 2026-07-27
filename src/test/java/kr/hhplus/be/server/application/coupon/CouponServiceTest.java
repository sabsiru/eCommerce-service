package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponInventoryReader couponInventoryReader;

    @Test
    void 쿠폰_단건조회_성공() {
        Long couponId = 1L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .name("테스트쿠폰")
                .discountRate(10)
                .maxDiscountAmount(5000)
                .status(CouponStatus.ACTIVE)
                .expirationAt(LocalDateTime.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .limitCount(10)
                .issuedCount(0)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        Coupon result = couponService.getCouponOrThrow(couponId);

        assertEquals(coupon, result);
        verify(couponRepository).findById(couponId);
    }

    @Test
    void 쿠폰_단건조회_실패() {
        Long couponId = 999L;
        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> couponService.getCouponOrThrow(couponId));
    }

    @Test
    void 쿠폰_발급_성공() {
        Long userId = 1L;
        Long couponId = 1L;
        UserCoupon issued = UserCoupon.issue(couponId, userId);

        when(couponInventoryReader.issue(couponId, userId)).thenReturn(true);
        when(userCouponRepository.save(any())).thenReturn(issued);

        UserCoupon result = couponService.issue(userId, couponId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(couponId, result.getCouponId());
        assertEquals(UserCouponStatus.ISSUED, result.getStatus());
        verify(couponInventoryReader).issue(couponId, userId);
        verify(userCouponRepository).save(any());
    }

    @Test
    void 쿠폰_발급_실패_수량초과() {
        Long userId = 1L;
        Long couponId = 1L;

        when(couponInventoryReader.issue(couponId, userId))
                .thenThrow(new IllegalStateException("재고가 소진되었습니다."));

        assertThrows(IllegalStateException.class, () -> couponService.issue(userId, couponId));
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    void 쿠폰_발급_실패_만료() {
        Long userId = 1L;
        Long couponId = 1L;

        when(couponInventoryReader.issue(couponId, userId))
                .thenThrow(new IllegalStateException("발급이 종료된 쿠폰입니다."));

        assertThrows(IllegalStateException.class, () -> couponService.issue(userId, couponId));
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    void 쿠폰_발급_저장실패시_재고를_복구한다() {
        Long userId = 1L;
        Long couponId = 1L;

        when(couponInventoryReader.issue(couponId, userId)).thenReturn(true);
        when(userCouponRepository.save(any())).thenThrow(new RuntimeException("저장 실패"));

        assertThrows(RuntimeException.class, () -> couponService.issue(userId, couponId));

        verify(couponInventoryReader).release(couponId, userId);
    }
}
