package kr.hhplus.be.server.interfaces.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.PopularProductSummary;
import kr.hhplus.be.server.domain.product.PopularProductSummaryRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private PopularProductSummaryRepository summaryRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 인기상품_정상_조회() throws Exception {
        LocalDate summaryDate = LocalDate.now().minusDays(1);
        List<PopularProductSummary> summaries = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new PopularProductSummary(
                        (long) i,           // productId
                        11 - i,             // totalQuantity: 10,9,8,7,6
                        summaryDate         // summaryDate is LocalDate
                ))
                .collect(Collectors.toList());
        summaryRepository.saveAll(summaries);

        // when & then
        mockMvc.perform(get("/products/popular")
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
        int offset = 0;
        int limit  = 20;

        mockMvc.perform(get("/products/latest")
                        .param("offset", String.valueOf(offset))
                        .param("limit",  String.valueOf(limit))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", lessThanOrEqualTo(limit)));
    }
}