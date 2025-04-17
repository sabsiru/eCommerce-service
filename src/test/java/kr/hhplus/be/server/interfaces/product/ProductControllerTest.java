package kr.hhplus.be.server.interfaces.product;

import kr.hhplus.be.server.application.product.ProductFacade;
import kr.hhplus.be.server.domain.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductFacade productFacade;

    @Test
    void 전체상품_정상조회() throws Exception {
        // Arrange: Product 객체들을 생성 (예: 2개의 상품)
        LocalDateTime now = LocalDateTime.now();
        Product product1 = new Product(1L, "Product A", 10000, 50, 1L, now, now);
        Product product2 = new Product(2L, "Product B", 15000, 30, 2L, now, now);
        List<Product> productList = Arrays.asList(product1, product2);

        // Stub: ProductService.getAllProducts() 호출 시, 위 productList 반환하도록 설정
        when(productFacade.getAllProducts()).thenReturn(productList);

        // Act & Assert:
        mockMvc.perform(get("/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Product A")))
                .andExpect(jsonPath("$[0].price", is(10000)))
                .andExpect(jsonPath("$[0].stock", is(50)))
                .andExpect(jsonPath("$[0].categoryId", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Product B")))
                .andExpect(jsonPath("$[1].price", is(15000)))
                .andExpect(jsonPath("$[1].stock", is(30)))
                .andExpect(jsonPath("$[1].categoryId", is(2)));
    }
}