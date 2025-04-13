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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Test
    void 쿠폰_단건조회_성공() {
        // given
        Long couponId = 1L;
        Coupon coupon = new Coupon(
                couponId,
                "테스트쿠폰",
                10,                    // 할인율
                5000,                  // 최대 할인금액
                CouponStatus.ACTIVE,   // 상태
                LocalDateTime.now().plusDays(5), // 만료일
                LocalDateTime.now(),   // 생성일
                10,                    // 발급 한도
                0                     // 현재 발급 수량 (한도 도달)
        );

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when
        Coupon result = couponService.getCouponOrThrow(couponId);

        // then
        assertEquals(coupon, result);
        verify(couponRepository, times(1)).findById(couponId);
    }

    @Test
    void 쿠폰_단건조회_실패() {
        // given
        Long couponId = 999L;
        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> couponService.getCouponOrThrow(couponId));
    }

    @Test
    void 쿠폰_발급_성공() {
        // given
        Long couponId = 1L;
        Coupon coupon = new Coupon(
                couponId,
                "테스트쿠폰",
                10,                    // 할인율
                5000,                  // 최대 할인금액
                CouponStatus.ACTIVE,   // 상태
                LocalDateTime.now().plusDays(5), // 만료일
                LocalDateTime.now(),   // 생성일
                10,                    // 발급 한도
                0                     // 현재 발급 수량 (한도 도달)
        );
        Coupon updated = coupon.increaseIssuedCount();  // 수량 1 증가
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any())).thenReturn(updated);

        // when
        Coupon result = couponService.issueCoupon(couponId);

        // then
        assertEquals(1, result.issuedCount());
        verify(couponRepository).findById(couponId);
        verify(couponRepository).save(any());
    }

    @Test
    void 쿠폰_발급_실패_수량초과() {
        // given
        Long couponId = 1L;
        Coupon coupon = new Coupon(
                couponId,
                "테스트쿠폰",
                10,                    // 할인율
                5000,                  // 최대 할인금액
                CouponStatus.ACTIVE,   // 상태
                LocalDateTime.now().plusDays(5), // 만료일
                LocalDateTime.now(),   // 생성일
                10,                    // 발급 한도
                10                     // 현재 발급 수량 (한도 도달)
        );
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when & then
        assertThrows(IllegalStateException.class, () -> couponService.issueCoupon(couponId));
    }

    @Test
    void 쿠폰_발급_실패_만료() {
        // given
        Long couponId = 1L;
        Coupon expiredCoupon = new Coupon(
                couponId, "만료쿠폰", 10, 5000,
                CouponStatus.ACTIVE,
                LocalDateTime.now().minusDays(1), // 만료
                LocalDateTime.now().minusDays(10),
                10, 5
        );
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(expiredCoupon));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> couponService.issueCoupon(couponId));
    }

    @Test
    @DisplayName("쿠폰 발급 수량이 한도에 도달할 때 - 상태 자동 EXPIRED 전환")
    void 쿠폰_발급_경계값_도달_상태_변경() {
        // given
        Coupon originalCoupon = new Coupon(
                1L,
                "10% 할인",
                10,
                5000,
                CouponStatus.ACTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now(),
                10,
                9
        );

        Coupon updatedCoupon = new Coupon(
                1L,
                "10% 할인",
                10,
                5000,
                CouponStatus.EXPIRED,  // 상태가 EXPIRED로 전환
                originalCoupon.expirationAt(),
                originalCoupon.createdAt(),
                10,
                10
        );

        when(couponRepository.findById(1L)).thenReturn(Optional.of(originalCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(updatedCoupon);

        // when
        Coupon result = couponService.issueCoupon(1L);

        // then
        assertEquals(10, result.issuedCount());
        assertEquals(CouponStatus.EXPIRED, result.status());
    }

    @Test
    @DisplayName("쿠폰 발급 한도 직전 상태 유지")
    void 쿠폰_발급_한도_직전_상태_유지() {
        // given
        Coupon originalCoupon = new Coupon(
                1L, "10% 할인", 10, 5000,
                CouponStatus.ACTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now(),
                5, 3
        );

        Coupon updatedCoupon = new Coupon(
                1L, "10% 할인", 10, 5000,
                CouponStatus.ACTIVE, // 발급 한도 직전이므로 상태는 유지
                originalCoupon.expirationAt(),
                originalCoupon.createdAt(),
                originalCoupon.limitCount(),
                originalCoupon.issuedCount() + 1
        );

        when(couponRepository.findById(1L)).thenReturn(Optional.of(originalCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(updatedCoupon);

        // when
        Coupon result = couponService.issueCoupon(1L);

        // then
        assertEquals(4, result.issuedCount());
        assertEquals(CouponStatus.ACTIVE, result.status());
    }


}