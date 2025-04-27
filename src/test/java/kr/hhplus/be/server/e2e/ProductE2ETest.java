package kr.hhplus.be.server.e2e;

import kr.hhplus.be.server.application.product.PopularProductInfo;
import kr.hhplus.be.server.application.product.PopularProductSummaryBatchService;
import kr.hhplus.be.server.domain.order.*;
import kr.hhplus.be.server.domain.product.PopularProductSummary;
import kr.hhplus.be.server.domain.product.PopularProductSummaryRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.infrastructure.product.ProductSummaryRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ProductE2ETest {
    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    PopularProductSummaryRepository popularProductSummaryRepository;

    @Autowired
    PopularProductSummaryBatchService batchService;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        popularProductSummaryRepository.deleteAll();
    }

    @Test
    void 상품_전체_페이징_조회_정상() {
        // given
        IntStream.range(1, 31).forEach(i ->
                productRepository.save(new Product("상품" + i, 1000 + i, 100, 1L))
        );

        // when
        ResponseEntity<ProductSummaryRow[]> response = restTemplate.getForEntity(
                "/products/latest?offset=0&limit=20",
                ProductSummaryRow[].class
        );

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(20);
    }

    @Test
    void 인기상품_조회_배치_포함_정상동작() {
        // given
        User user = userRepository.save(User.create("user", 100000));

        List<Product> products = new ArrayList<>();
        IntStream.rangeClosed(1, 10).forEach(i -> {
            Product product = new Product("상품" + i, 1000 * i, 100, 1L);
            products.add(productRepository.save(product));
        });

        // 테스트를 위하여 편의상 builder 사용(테스트 이후에는 @builder 사용 x)
        for (int i = 0; i < 10; i++) {
            Product product = products.get(i);
            Order order = Order.builder()
                    .userId(user.getId())
                    .totalAmount(product.getPrice() * (10 - i))
                    .status(OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();
            order = orderRepository.save(order);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .quantity(10 - i)
                    .orderPrice(product.getPrice())
                    .build();
            orderItemRepository.save(item);
        }

        // when
        batchService.updateSummary(LocalDateTime.now());

        // then
        ResponseEntity<PopularProductInfo[]> response = restTemplate.getForEntity(
                "/products/popular",
                PopularProductInfo[].class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        PopularProductInfo[] popularProducts = response.getBody();
        assertThat(popularProducts).isNotNull();
        assertThat(popularProducts).hasSize(5);

        List<Long> expectedIds = popularProductSummaryRepository.findAll().stream()
                .sorted(Comparator.comparing(PopularProductSummary::getTotalQuantity).reversed())
                .map(PopularProductSummary::getProductId)
                .toList();

        List<Long> resultIds = List.of(popularProducts).stream()
                .map(PopularProductInfo::getProductId)
                .toList();

        assertThat(resultIds).isEqualTo(expectedIds);
    }
}
