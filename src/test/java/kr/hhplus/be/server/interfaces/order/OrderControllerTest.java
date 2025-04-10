package kr.hhplus.be.server.interfaces.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.order.CreateOrderRequest;
import kr.hhplus.be.server.application.order.OrderItemRequest;
import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.application.order.PopularProductRequest;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderFacade orderFacade;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 주문_생성_성공() throws Exception {
        // given
        Long userId = 1L;
        List<OrderItemRequest> itemRequests = List.of(new OrderItemRequest(101L, 2, 5000));
        CreateOrderRequest request = new CreateOrderRequest(userId, itemRequests);

        OrderItemResponse itemResponse = new OrderItemResponse(101L, 2, 5000);
        OrderResponse orderResponse = new OrderResponse(1L, userId, List.of(itemResponse), 10_000, OrderStatus.PENDING);

        when(orderFacade.processOrder(any(), any())).thenReturn(orderResponse);

        // when & then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andDo(print())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items[0].productId").value(101L))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].orderPrice").value(5000))
                .andExpect(jsonPath("$.totalAmount").value(10_000))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 사용자_주문_조회_성공() throws Exception {
        // given
        Long userId = 1L;
        Order order = new Order(1L, userId,
                List.of(new OrderItem(1L, 1L, 101L, 2, 5000, LocalDateTime.now())),
                10000, OrderStatus.PENDING, LocalDateTime.now(), LocalDateTime.now());

        when(orderFacade.getOrdersByUser(userId)).thenReturn(List.of(order));

        // when & then
        mockMvc.perform(get("/orders/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.id()))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void 주문_취소_성공() throws Exception {
        // given
        Long orderId = 1L;
        Order canceledOrder = new Order(orderId, 1L,
                List.of(new OrderItem(1L, orderId, 101L, 2, 5000, LocalDateTime.now())),
                10000, OrderStatus.CANCEL, LocalDateTime.now(), LocalDateTime.now());

        when(orderFacade.cancelOrder(orderId)).thenReturn(canceledOrder);

        // when & then
        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCEL"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품 재고 부족")
    void 주문_생성_실패_재고부족() throws Exception {
        // given
        CreateOrderRequest request = new CreateOrderRequest(1L,
                List.of(new OrderItemRequest(101L, 100, 5000))); // 재고보다 많은 수량

        Product product = new Product(101L, "상품", 10000, 0, 1L, LocalDateTime.now(), LocalDateTime.now()); // 재고 0
        when(productService.getProductOrThrow(101L)).thenReturn(product);
        when(orderFacade.processOrder(anyLong(), anyList()))
                .thenThrow(new IllegalStateException("재고가 부족합니다."));

        // when & then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("재고가 부족합니다.")));
    }

    @Test
    void 사용자_주문조회_실패_유저없음() throws Exception {
        Long invalidUserId = 999L;
        when(orderFacade.getOrdersByUser(invalidUserId))
                .thenThrow(new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        mockMvc.perform(get("/orders/{userId}", invalidUserId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("사용자를 찾을 수 없습니다.")));
    }

    @Test
    @DisplayName("주문 취소 실패 - 주문이 존재하지 않음")
    void 주문_취소_실패_존재하지않는_주문() throws Exception {
        Long invalidOrderId = 999L;
        when(orderFacade.cancelOrder(invalidOrderId))
                .thenThrow(new IllegalArgumentException("주문을 찾을 수 없습니다."));

        mockMvc.perform(patch("/orders/{orderId}/cancel", invalidOrderId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("주문을 찾을 수 없습니다.")));
    }

    @Test
    @DisplayName("주문 취소 실패 - 결제 완료 상태")
    void 주문_취소_실패_결제완료상태() throws Exception {
        Long paidOrderId = 2L;
        when(orderFacade.cancelOrder(paidOrderId))
                .thenThrow(new IllegalStateException("이미 결제 완료된 주문은 취소할 수 없습니다."));

        mockMvc.perform(patch("/orders/{orderId}/cancel", paidOrderId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("이미 결제 완료된 주문은 취소할 수 없습니다.")));
    }

    @Test
    @DisplayName("인기 상품 조회 - 성공")
    void 인기_상품_조회_성공() throws Exception {
        // given
        List<PopularProductRequest> popularProducts = List.of(
                new PopularProductRequest(101L, 15),
                new PopularProductRequest(102L, 12),
                new PopularProductRequest(103L, 10),
                new PopularProductRequest(104L, 8),
                new PopularProductRequest(105L, 5)
        );
        when(orderFacade.getPopularProduct()).thenReturn(popularProducts);

        // when & then
        mockMvc.perform(get("/orders/popular-products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(101L))
                .andExpect(jsonPath("$[0].totalQuantity").value(15))
                .andExpect(jsonPath("$[1].productId").value(102L))
                .andExpect(jsonPath("$[1].totalQuantity").value(12));
    }

}