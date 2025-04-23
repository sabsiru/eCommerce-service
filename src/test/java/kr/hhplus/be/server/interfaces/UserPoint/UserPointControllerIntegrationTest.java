package kr.hhplus.be.server.interfaces.UserPoint;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPointControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 포인트_충전_성공() throws Exception {
        // given: 유저 등록
        User user = userRepository.save(User.create("테스트유저", 0));

        UserPointRequest request = new UserPointRequest(user.getId(), 5000);

        // when & then
        mockMvc.perform(post("/point/{userId}/charge", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.point").value(5000))
                .andExpect(jsonPath("$.name").value("테스트유저"));
    }

    @Test
    void 포인트_충전_실패_음수요청() throws Exception {
        User user = userRepository.save(User.create("에러유저", 0));
        UserPointRequest request = new UserPointRequest(user.getId(), -1000);

        mockMvc.perform(post("/point/{userId}/charge", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("충전 금액은 0보다 커야 합니다")));
    }

    @Test
    void 보유포인트_최대한도_초과_예외() throws Exception {
        User user = userRepository.save(User.create("최대한도유저", 9_999_999));
        UserPointRequest request = new UserPointRequest(user.getId(), 10_000);  // 총합 1000만 초과

        mockMvc.perform(post("/point/{userId}/charge", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("최대 충전 한도는 `10,000,000원` 입니다.")));
    }

    @Test
    void 최대_1회_충전_금액_초과_예외() throws Exception {
        User user = userRepository.save(User.create("1회한도유저", 0));
        UserPointRequest request = new UserPointRequest(user.getId(), 1_000_001);  // 1회 한도 초과

        mockMvc.perform(post("/point/{userId}/charge", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("1회 충전 금액은 `1,000,000원` 입니다.")));
    }
}
