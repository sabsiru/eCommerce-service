package kr.hhplus.be.server.interfaces.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.order.CreateOrderCommand;
import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        Long userId = 1L;
        List<OrderItemCommand> itemRequests = List.of(new OrderItemCommand(101L, 2, 5000));
        CreateOrderCommand request = new CreateOrderCommand();
        request.setUserId(userId);
        request.setOrderItemCommands(itemRequests);

        OrderItemResponse itemResponse = new OrderItemResponse(101L, 2, 5000);
        OrderResponse orderResponse = new OrderResponse(1L, userId, List.of(itemResponse), 10_000, OrderStatus.PENDING);

        when(orderFacade.processOrder(any(CreateOrderCommand.class))).thenReturn(orderResponse);

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
        Long userId = 1L;
        Order order = Order.builder()
                .id(1L)
                .userId(userId)
                .totalAmount(10000)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(List.of(OrderItem.create(mock(Order.class), 101L, 1, 10000)))
                .build();
        when(orderFacade.getOrdersByUser(userId)).thenReturn(List.of(order));
        mockMvc.perform(get("/orders/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void 주문_취소_성공() throws Exception {
        Long orderId = 1L;
        Long userId = 1L;

        Order canceledOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .totalAmount(10000)
                .status(OrderStatus.CANCEL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(List.of(OrderItem.create(mock(Order.class), 101L, 1, 10000)))
                .build();

        when(orderFacade.cancelOrder(orderId)).thenReturn(canceledOrder);

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCEL"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품 재고 부족")
    void 주문_생성_실패_재고부족() throws Exception {
        CreateOrderCommand request = new CreateOrderCommand();
        request.setUserId(1L);
        request.setOrderItemCommands(List.of(new OrderItemCommand(101L, 100, 5000)));

        Product product = new Product(101L, "상품", 10000, 0, 1L, LocalDateTime.now(), LocalDateTime.now());
        when(productService.getProductOrThrow(101L)).thenReturn(product);
        when(orderFacade.processOrder(any(CreateOrderCommand.class)))
                .thenThrow(new IllegalStateException("재고가 부족합니다."));

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
}
