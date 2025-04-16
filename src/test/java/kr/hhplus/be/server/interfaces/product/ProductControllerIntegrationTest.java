package kr.hhplus.be.server.interfaces.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
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
import org.springframework.test.annotation.Commit;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void 인기상품_정상_조회() throws Exception {
        // given
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            products.add(productRepository.save(new Product("상품" + i, 1000 * i, 100, 1L)));
        }

        // 각 상품별로 서로 다른 판매 수량 설정 (10개, 9개, ..., 1개)
        List<OrderItemCommand> itemCommands = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            itemCommands.add(new OrderItemCommand(products.get(i).getId(), 10 - i, products.get(i).getPrice()));
        }

        CreateOrderCommand command = new CreateOrderCommand(1L, itemCommands);
        Order order = Order.create(command.getUserId(), command.getOrderItemCommands());

        orderRepository.saveAndFlush(order);

        // when & then
        mockMvc.perform(get("/products/popular")
                        .param("fromDate", LocalDateTime.now().minusDays(3).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].totalQuantity").value(10))
                .andExpect(jsonPath("$[1].totalQuantity").value(9))
                .andExpect(jsonPath("$[2].totalQuantity").value(8))
                .andExpect(jsonPath("$[3].totalQuantity").value(7))
                .andExpect(jsonPath("$[4].totalQuantity").value(6));
    }

    @Test
    void 전체_상품_조회_성공() throws Exception {
        // given
        productRepository.save(new Product("상품1", 1000, 10, 1L));
        productRepository.save(new Product("상품2", 2000, 20, 2L));
        productRepository.save(new Product("상품3", 3000, 30, 3L));

        // when & then
        mockMvc.perform(get("/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("상품1"))
                .andExpect(jsonPath("$[1].name").value("상품2"))
                .andExpect(jsonPath("$[2].name").value("상품3"));
    }
}