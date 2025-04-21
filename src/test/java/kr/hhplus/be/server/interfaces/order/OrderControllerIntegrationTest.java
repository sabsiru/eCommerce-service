package kr.hhplus.be.server.interfaces.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    @Test
    void 주문_생성_성공() throws Exception {
        User user = userRepository.save(User.create("사용자", 100000));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        String requestJson = """
            {
              "userId": %d,
              "items": [
                { "productId": %d, "quantity": 2, "itemPrice": 10000 }
              ]
            }
        """.formatted(user.getId(), product.getId());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(20000))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 주문_취소_성공() throws Exception {
        User user = userRepository.save(User.create("취소유저", 0));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        Order order = new Order(user.getId());
        order.addLine(product.getId(), 1, 10000);
        order = orderRepository.save(order);
        orderItemRepository.saveAll(order.getItems());

        mockMvc.perform(patch("/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("CANCEL"));
    }

    @Test
    void 사용자_주문조회_성공() throws Exception {
        User user = userRepository.save(User.create("조회유저", 0));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        Order order = new Order(user.getId());
        order.addLine(product.getId(), 2, 10000);
        order = orderRepository.save(order);
        orderItemRepository.saveAll(order.getItems());

        mockMvc.perform(get("/orders/{userId}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(user.getId()))
                .andExpect(jsonPath("$[0].items", hasSize(1)))
                .andExpect(jsonPath("$[0].totalAmount").value(20000));
    }

    @Test
    void 주문_취소_실패_결제완료상태() throws Exception {
        User user = userRepository.save(User.create("결제유저", 0));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));


        Order order = new Order(user.getId());
        order.addLine(product.getId(), 1, 10000);
        order.pay();
        order = orderRepository.save(order);
        orderItemRepository.saveAll(order.getItems());
        mockMvc.perform(patch("/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("이미 결제 완료된 주문은 취소할 수 없습니다")));
    }

    @Test
    void 주문_생성_실패_항목없음() throws Exception {
        User user = userRepository.save(User.create("빈주문자", 0));

        String requestJson = """
            {
              "userId": %d,
              "items": []
            }
        """.formatted(user.getId());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("주문 항목이 비어 있습니다.")));
    }

    @Test
    void 주문_취소_실패_주문없음() throws Exception {
        mockMvc.perform(patch("/orders/{orderId}/cancel", 99999L))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("주문을 찾을 수 없습니다")));
    }

    @Test
    void 사용자_주문조회_실패_유저없음() throws Exception {
        mockMvc.perform(get("/orders/{userId}", 99999L))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("해당 유저가 없거나 주문 목록이 없습니다.")));
    }
}
