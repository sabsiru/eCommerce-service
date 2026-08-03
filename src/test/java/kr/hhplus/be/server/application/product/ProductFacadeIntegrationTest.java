package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItemRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductFacadeIntegrationTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductFacade productFacade;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void cleanDb() {
        // OrderItem.order는 FK 제약이 없어(NO_CONSTRAINT) orderRepository.deleteAll()로
        // 자동 삭제되지 않는다 - 명시적으로 같이 지워야 다른 테스트의 주문 데이터가 집계에 안 섞인다.
        orderItemRepository.deleteAll();
        productRepository.deleteAll();
        orderRepository.deleteAll();

        // productFacade.getPopularProducts()는 고정 키("top5")로 캐싱되는데, 로컬처럼
        // Redis가 여러 번의 테스트 실행 사이에도 계속 떠있으면 이전 실행에서 캐싱된
        // 값이 이번 실행의 새 데이터와 어긋나 테스트가 흔들릴 수 있다 - 매번 비워준다.
        var cache = cacheManager.getCache("popularProducts");
        if (cache != null) {
            cache.clear();
        }
    }
    @Test
    void 인기_상품_조회_성공() {
        // given: 7개 상품을 명확한 수량으로 주문
        Long userId = 1L;
        Product p1 = productRepository.save(new Product("상품1", 1000, 100, userId)); // 5개
        Product p2 = productRepository.save(new Product("상품2", 1000, 100, userId)); // 3개
        Product p3 = productRepository.save(new Product("상품3", 1000, 100, userId)); // 7개
        Product p4 = productRepository.save(new Product("상품4", 1000, 100, userId)); // 9개
        Product p5 = productRepository.save(new Product("상품5", 1000, 100, userId)); // 2개
        Product p6 = productRepository.save(new Product("상품6", 1000, 100, userId)); // 6개
        Product p7 = productRepository.save(new Product("상품7", 1000, 100, userId)); // 1개

        // 수량 설정
        Map<Product, Integer> orderMap = Map.of(
                p1, 5,
                p2, 3,
                p3, 7,
                p4, 9,
                p5, 2,
                p6, 6,
                p7, 1
        );

        // 주문 저장
        for (Map.Entry<Product, Integer> entry : orderMap.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            Order order = new Order(userId);
            order.addLine(product.getId(), quantity, product.getPrice());
            orderRepository.save(order);
            orderItemRepository.saveAll(order.getItems());
        }

        // when
        List<PopularProductInfo> result = productFacade.getPopularProducts();

        // then
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getProductId()).isEqualTo(p4.getId()); // 9개
        assertThat(result.get(1).getProductId()).isEqualTo(p3.getId()); // 7개
        assertThat(result.get(2).getProductId()).isEqualTo(p6.getId()); // 6개
        assertThat(result.get(3).getProductId()).isEqualTo(p1.getId()); // 5개
        assertThat(result.get(4).getProductId()).isEqualTo(p2.getId()); // 3개
    }
}