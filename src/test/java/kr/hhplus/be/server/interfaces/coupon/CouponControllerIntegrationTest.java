package kr.hhplus.be.server.interfaces.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 쿠폰_발급_성공() throws Exception {
        User user = userRepository.save(User.create("쿠폰유저", 0));
        Coupon coupon = couponService.create("10%할인", 10, 3000, LocalDateTime.now().plusDays(2), 100);

        mockMvc.perform(post("/coupons/{userId}/issue", user.getId())
                        .param("couponId", String.valueOf(coupon.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.couponId").value(coupon.getId()));
    }

    @Test
    void 쿠폰_조회_성공() throws Exception {
        Coupon coupon = couponRepository.save(Coupon.create("20%할인", 20, 5000, LocalDateTime.now().plusDays(3), 100));

        mockMvc.perform(get("/coupons")
                        .param("couponId", String.valueOf(coupon.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupon.getId()))
                .andExpect(jsonPath("$.name").value("20%할인"));
    }

    @Test
    void 쿠폰_조회_실패_존재하지않음() throws Exception {
        mockMvc.perform(get("/coupons")
                        .param("couponId", "99999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("쿠폰을 찾을 수 없습니다.")));
    }

    @Test
    void 쿠폰_발급_수량소진시_상태변경_검증() throws Exception {
        // given: 발급 수량 1개인 쿠폰 생성
        User user1 = userRepository.save(User.create("user1", 0));
        User user2 = userRepository.save(User.create("user2", 0));
        Coupon coupon = couponService.create("단일사용쿠폰", 30, 3000, LocalDateTime.now().plusDays(3), 1);

        // when: 첫 번째 사용자 발급
        mockMvc.perform(post("/coupons/{userId}/issue", user1.getId())
                        .param("couponId", String.valueOf(coupon.getId())))
                .andExpect(status().isCreated());

        // 추가 발급 시도 → 실패 (재고 소진, Redis 인벤토리 키 자체가 사라짐)
        mockMvc.perform(post("/coupons/{userId}/issue", user2.getId())
                        .param("couponId", String.valueOf(coupon.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("발급이 종료된 쿠폰입니다.")));
    }

    @Test
    void 동일_유저_중복_쿠폰_발급_실패() throws Exception {
        User user = userRepository.save(User.create("중복테스터", 0));
        Coupon coupon = couponService.create("테스트쿠폰", 10, 2000, LocalDateTime.now().plusDays(3), 5);

        // 최초 발급
        mockMvc.perform(post("/coupons/{userId}/issue", user.getId())
                        .param("couponId", String.valueOf(coupon.getId())))
                .andExpect(status().isCreated());

        // 중복 발급 시도
        mockMvc.perform(post("/coupons/{userId}/issue", user.getId())
                        .param("couponId", String.valueOf(coupon.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("이미 발급받은 사용자입니다.")));
    }

    @Test
    void 만료된_쿠폰_발급_실패() throws Exception {
        // given
        User user = userRepository.save(User.create("만료쿠폰유저", 0));
        Coupon expiredCoupon = couponService.create("만료쿠폰", 20, 5000, LocalDateTime.now().minusDays(1), 100);

        // when & then
        mockMvc.perform(post("/coupons/{userId}/issue", user.getId())
                        .param("couponId", expiredCoupon.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("발급이 종료된 쿠폰입니다.")));
    }
}