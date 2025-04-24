package kr.hhplus.be.server.e2e;

import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.interfaces.UserPoint.UserPointRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserPointE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void 유저_포인트_충전_성공() {
        // given
        User user = User.create("testUser", 0);
        user = userRepository.save(user);

        // when
        UserPointRequest request = new UserPointRequest(user.getId(), 1000);
        ResponseEntity<User> response = restTemplate.postForEntity(
                "/point/charge",
                request,
                User.class
        );

        // then: HTTP 200
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Optional<User> updated = userRepository.findById(user.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getPoint()).isEqualTo(1000);
    }

    @Test
    void 존재하지_않는_유저_충전_실패() {
        // when: 없는 유저 ID로 요청
        UserPointRequest req = new UserPointRequest(999L, 500);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/point/charge",
                req,
                String.class
        );

        // then: 404 NOT_FOUND
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void 음수_금액_충전_실패() {
        // given: 테스트용 유저 생성
        User user = userRepository.save(User.create("testUser", 0));

        // when: 음수 금액으로 요청
        UserPointRequest req = new UserPointRequest(user.getId(), -500);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/point/charge",
                req,
                String.class
        );

        // then: 400 BAD_REQUEST
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void 일회_최대_초과_금액_충전_실패() {
        // given
        User u = userRepository.save(User.create("userMax", 0));

        // when
        UserPointRequest req = new UserPointRequest(u.getId(), 1_000_001);
        ResponseEntity<String> res = restTemplate.postForEntity("/point/charge",
                req,
                String.class
        );

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void 총_최대_초과_금액_충전_실패() {
        // given
        User u = userRepository.save(User.create("userTotalMax", 9_500_000));

        // when:
        UserPointRequest req = new UserPointRequest(u.getId(), 600_000);
        ResponseEntity<String> res = restTemplate.postForEntity(
                 "/point/charge",
                req,
                String.class
        );

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
