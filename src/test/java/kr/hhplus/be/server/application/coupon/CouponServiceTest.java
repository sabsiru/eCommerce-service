package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import org.junit.jupiter.api.DisplayName;
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

        Coupon updated = coupon.increaseIssuedCount();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any())).thenReturn(updated);

        Coupon result = couponService.issueCoupon(couponId);

        assertEquals(1, result.getIssuedCount());
        verify(couponRepository).findById(couponId);
        verify(couponRepository).save(any());
    }

    @Test
    void 쿠폰_발급_실패_수량초과() {
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
                .issuedCount(10)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        assertThrows(IllegalStateException.class, () -> couponService.issueCoupon(couponId));
    }

    @Test
    void 쿠폰_발급_실패_만료() {
        Long couponId = 1L;
        Coupon expiredCoupon = Coupon.builder()
                .id(couponId)
                .name("만료쿠폰")
                .discountRate(10)
                .maxDiscountAmount(5000)
                .status(CouponStatus.ACTIVE)
                .expirationAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(10))
                .limitCount(10)
                .issuedCount(5)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(expiredCoupon));

        assertThrows(IllegalArgumentException.class, () -> couponService.issueCoupon(couponId));
    }

    @Test
    @DisplayName("쿠폰 발급 수량이 한도에 도달할 때 - 상태 자동 EXPIRED 전환")
    void 쿠폰_발급_경계값_도달_상태_변경() {
        Coupon originalCoupon = Coupon.builder()
                .id(1L)
                .name("10% 할인")
                .discountRate(10)
                .maxDiscountAmount(5000)
                .status(CouponStatus.ACTIVE)
                .expirationAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .limitCount(10)
                .issuedCount(9)
                .build();

        Coupon updatedCoupon = originalCoupon.increaseIssuedCount();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(originalCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(updatedCoupon);

        Coupon result = couponService.issueCoupon(1L);

        assertEquals(10, result.getIssuedCount());
        assertEquals(CouponStatus.EXPIRED, result.getStatus());
    }

    @Test
    @DisplayName("쿠폰 발급 한도 직전 상태 유지")
    void 쿠폰_발급_한도_직전_상태_유지() {
        Coupon originalCoupon = Coupon.builder()
                .id(1L)
                .name("10% 할인")
                .discountRate(10)
                .maxDiscountAmount(5000)
                .status(CouponStatus.ACTIVE)
                .expirationAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .limitCount(5)
                .issuedCount(3)
                .build();

        Coupon updatedCoupon = originalCoupon.increaseIssuedCount();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(originalCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(updatedCoupon);

        Coupon result = couponService.issueCoupon(1L);

        assertEquals(4, result.getIssuedCount());
        assertEquals(CouponStatus.ACTIVE, result.getStatus());
    }
}
