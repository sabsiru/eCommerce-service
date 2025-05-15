package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.application.payment.PaymentFacade;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderLine;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PopularProductServiceIntegrationTest {

    @Autowired
    private PopularProductService popularProductService;
    @Autowired
    private PaymentFacade paymentFacade;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PRODUCT_SALES_KEY = "product:sales:daily";

    @BeforeEach
    void setUp() {
        // Redis 데이터 초기화
        redisTemplate.delete(redisTemplate.keys(PRODUCT_SALES_KEY + "*"));
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 최근_3일간_인기상품_조회_성공() {
        // given
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);
        String twoDaysAgo = LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_DATE);

        // 오늘 판매 데이터
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + today, "1", 5);
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + today, "2", 3);
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + today, "3", 4);

        // 어제 판매 데이터
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + yesterday, "1", 2);
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + yesterday, "2", 6);
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + yesterday, "4", 3);

        // 2일 전 판매 데이터
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + twoDaysAgo, "1", 3);
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + twoDaysAgo, "3", 2);
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + twoDaysAgo, "5", 4);

        // when
        List<PopularProductInfo> result = popularProductService.getPopularProductsRedis();

        // then
        assertThat(result).hasSize(5);

        assertThat(result.get(0).getProductId()).isEqualTo(1L);  // 10점
        assertThat(result.get(0).getTotalQuantity()).isEqualTo(10);

        assertThat(result.get(1).getProductId()).isEqualTo(2L);  // 9점
        assertThat(result.get(1).getTotalQuantity()).isEqualTo(9);

        assertThat(result.get(2).getProductId()).isEqualTo(3L);  // 6점
        assertThat(result.get(2).getTotalQuantity()).isEqualTo(6);
    }

    @Test
    void 데이터가_없을때_빈_리스트_반환() {
        // when
        List<PopularProductInfo> result = popularProductService.getPopularProductsRedis();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 임시키_정상_삭제_확인() {
        // given
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        redisTemplate.opsForZSet().add(PRODUCT_SALES_KEY + ":" + today, "1", 5);

        // when
        popularProductService.getPopularProductsRedis();

        // then
        Boolean exists = redisTemplate.hasKey(PRODUCT_SALES_KEY + ":temp");
        assertThat(exists).isFalse();
    }

    @Test
    void 결제_후_Redis_랭킹_정보_검증() {
        // given
        User user = userRepository.save(User.create("테스터", 1000000));
        Product product1 = productRepository.save(new Product("상품1", 10000, 100, 1L));
        Product product2 = productRepository.save(new Product("상품2", 20000, 100, 2L));

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String redisKey = PRODUCT_SALES_KEY + ":" + today;

        // when
        createAndProcessOrder(user.getId(), product1.getId(), 2);
        Double product1Score = redisTemplate.opsForZSet().score(redisKey, product1.getId().toString());

        createAndProcessOrder(user.getId(), product2.getId(), 1);
        Double product2Score = redisTemplate.opsForZSet().score(redisKey, product2.getId().toString());

        List<PopularProductInfo> result = popularProductService.getPopularProductsRedis();

        // then
        assertThat(product1Score).isEqualTo(2.0);
        assertThat(product2Score).isEqualTo(1.0);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(product1.getId());
        assertThat(result.get(0).getTotalQuantity()).isEqualTo(2);
        assertThat(result.get(1).getProductId()).isEqualTo(product2.getId());
        assertThat(result.get(1).getTotalQuantity()).isEqualTo(1);
    }


    private void createAndProcessOrder(Long userId, Long productId, int quantity) {
        List<OrderLine> lines = List.of(
                new OrderLine(productId, quantity,
                        productRepository.findById(productId).orElseThrow().getPrice())
        );
        Order order = orderService.create(userId, lines);
        paymentFacade.processPayment(order.getId(), order.getTotalAmount());
    }
}