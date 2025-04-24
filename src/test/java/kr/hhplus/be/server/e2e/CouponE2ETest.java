package kr.hhplus.be.server.e2e;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CouponE2ETest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 쿠폰_정상_발급_테스트() {
        // given
        User user = userRepository.save(User.create("발급유저", 10000));
        Coupon coupon = couponRepository.save(Coupon.create("테스트쿠폰", 20, 2000, LocalDateTime.now().plusDays(1), 100));

        // when: 발급 API 호출
        ResponseEntity<String> issueRes = restTemplate.postForEntity(
                "/coupons/" + user.getId() + "/issue?couponId=" + coupon.getId(),
                null,
                String.class
        );

        // then
        assertThat(issueRes.getStatusCode().is2xxSuccessful()).isTrue();

        List<UserCoupon> userCoupons = userCouponRepository.findAll();
        assertThat(userCoupons).hasSize(1);
        assertThat(userCoupons.get(0).getUserId()).isEqualTo(user.getId());
        assertThat(userCoupons.get(0).getId()).isEqualTo(coupon.getId());
    }
}
