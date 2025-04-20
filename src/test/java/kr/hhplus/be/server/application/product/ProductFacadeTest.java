package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductFacadeTest {

    @InjectMocks
    private ProductFacade productFacade;

    @Mock
    private PopularProductService popularProductService;

    @Mock
    private ProductService productService; // 필요 시만 사용

    @Test
    void 인기_상품_조회_성공() {
        // given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(3);
        List<PopularProductInfo> mockResult = List.of(
                new PopularProductInfo(1L, 30),
                new PopularProductInfo(2L, 20)
        );

        given(popularProductService.getPopularProducts()).willReturn(mockResult);

        // when
        List<PopularProductInfo> result = productFacade.getPopularProducts();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(1L);
        assertThat(result.get(0).getTotalQuantity()).isEqualTo(30L);
    }
}