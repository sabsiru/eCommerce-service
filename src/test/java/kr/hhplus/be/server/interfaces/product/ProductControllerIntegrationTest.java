package kr.hhplus.be.server.interfaces.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private OrderItemRepository orderItemRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 인기상품_정상_조회() throws Exception {
        // given
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            products.add(productRepository.save(new Product("상품" + i, 1000 * i, 100, 1L)));
        }

        // 각 상품별로 서로 다른 판매 수량 설정 (10개, 9개, ..., 1개)
        Order order = new Order(1L);
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            int qty = 10 - i;
            order.addLine(p.getId(), qty, p.getPrice());
        }

        orderRepository.saveAndFlush(order);
        orderItemRepository.saveAll(order.getItems());

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
    void 전체_상품_페이징_조회() throws Exception {
        int size = 20;

        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", lessThanOrEqualTo(size)))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }
}