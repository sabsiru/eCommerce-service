package kr.hhplus.be.server.interfaces.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.order.Order;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void 주문_생성_성공() throws Exception {
        User user = userRepository.save(User.create("주문자", 100000));
        Product p1 = productRepository.save(new Product("상품1", 10000, 10, 1L));

        CreateOrderCommand command = new CreateOrderCommand(user.getId(),
                List.of(new OrderItemCommand(p1.getId(), 2, p1.getPrice())));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(20000))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 사용자_주문조회_성공() throws Exception {
        User user = userRepository.save(User.create("사용자", 0));
        Product p1 = productRepository.save(new Product("상품1", 10000, 10, 1L));
        Product p2 = productRepository.save(new Product("상품2", 15000, 10, 1L));

        CreateOrderCommand command = new CreateOrderCommand(user.getId(), List.of(
                new OrderItemCommand(p1.getId(), 1, p1.getPrice()),
                new OrderItemCommand(p2.getId(), 2, p2.getPrice())
        ));
        Order order = orderRepository.save(Order.create(command.getUserId(), command.getOrderItemCommands()));
        orderItemRepository.saveAll(order.getItems());

        mockMvc.perform(get("/orders/{userId}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(user.getId()))
                .andExpect(jsonPath("$[0].items", hasSize(2)))
                .andExpect(jsonPath("$[0].totalAmount").value(10000 + 2 * 15000));
    }

    @Test
    void 주문_취소_성공() throws Exception {
        User user = userRepository.save(User.create("취소유저", 0));
        Product p = productRepository.save(new Product("상품", 10000, 10, 1L));

        CreateOrderCommand command = new CreateOrderCommand(user.getId(),
                List.of(new OrderItemCommand(p.getId(), 1, p.getPrice())));

        Order order = orderRepository.save(Order.create(command.getUserId(), command.getOrderItemCommands()));

        mockMvc.perform(patch("/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("CANCEL"));
    }

    @Test
    void 사용자_주문조회_실패_유저없음() throws Exception {
        Long 잘못된유저아이디 = 9999L;

        mockMvc.perform(get("/orders/{userId}", 잘못된유저아이디))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("해당 유저가 없거나 주문 목록이 없습니다.")));
    }

    @Test
    void 주문_취소_실패_주문없음() throws Exception {
        Long 잘못된주문아이디 = 9999L;

        mockMvc.perform(patch("/orders/{orderId}/cancel", 잘못된주문아이디))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("주문을 찾을 수 없습니다")));
    }
    @Test
    void 주문_취소_실패_결제완료상태() throws Exception {
        User user = userRepository.save(User.create("결제유저", 0));
        Product product = productRepository.save(new Product("상품", 10000, 10, 1L));

        CreateOrderCommand command = new CreateOrderCommand(user.getId(),
                List.of(new OrderItemCommand(product.getId(), 1, product.getPrice())));
        Order order = orderRepository.save(Order.create(command.getUserId(), command.getOrderItemCommands()));

        order.pay(); // 상태를 수동으로 결제 완료로 변경

        mockMvc.perform(patch("/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("이미 결제 완료된 주문은 취소할 수 없습니다")));
    }

    @Test
    void 주문_생성_실패_주문항목_없음() throws Exception {
        User user = userRepository.save(User.create("테스트유저", 0));
        CreateOrderCommand command = new CreateOrderCommand(user.getId(), List.of());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("주문 항목이 비어 있습니다.")));
    }
}
