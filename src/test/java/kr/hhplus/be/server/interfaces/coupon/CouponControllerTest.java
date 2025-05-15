package kr.hhplus.be.server.interfaces.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.coupon.CouponFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CouponController.class)
public class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponFacade couponFacade;

    @Test
    void 쿠폰단건조회_테스트() throws Exception {
        // given
        Long couponId = 500L;
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = new Coupon(
                couponId,
                "테스트 쿠폰",
                20,
                2000,
                CouponStatus.ACTIVE,
                now.plusDays(7),
                now,
                100,
                0
        );
        when(couponFacade.getCouponOrThrow(eq(couponId))).thenReturn(coupon);
        String expectedJson = objectMapper.writeValueAsString(coupon);

        // when & then
        mockMvc.perform(get("/coupons")
                        .param("couponId", couponId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void 쿠폰발급_테스트() throws Exception {
        // given
        Long userId = 100L;
        Long couponId = 500L;
        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon(
                1L,
                userId,
                couponId,
                UserCouponStatus.ISSUED,
                now,
                null
        );
        when(couponFacade.issue(eq(userId), eq(couponId))).thenReturn(userCoupon);
        String expectedJson = objectMapper.writeValueAsString(userCoupon);

        // when & then
        mockMvc.perform(post("/coupons/{userId}/issue", userId)
                        .param("couponId", couponId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void 쿠폰발급_중복_실패_테스트() throws Exception {
        // given
        Long userId = 100L;
        Long couponId = 500L;
        when(couponFacade.issue(eq(userId), eq(couponId)))
                .thenThrow(new IllegalStateException("이미 발급받은 쿠폰입니다."));

        String expectedMessage = "이미 발급받은 쿠폰입니다.";

        // when & then
        mockMvc.perform(post("/coupons/{userId}/issue", userId)
                        .param("couponId", couponId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(expectedMessage)));
    }
}