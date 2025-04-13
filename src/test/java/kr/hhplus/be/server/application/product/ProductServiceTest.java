package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    public void 재고_조회_정상_테스트() {
        // given
        Long productId = 1L;
        int expectedStock = 50;
        // Product 도메인: price와 stock은 int, id 및 categoryId는 long
        Product product = new Product(productId, "Test Product", 10000, expectedStock, 1L,  LocalDateTime.now(), LocalDateTime.now());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        int actualStock = productService.getStock(productId);

        // then
        assertEquals(expectedStock, actualStock, "조회된 재고량이 예상값과 일치해야 합니다.");
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    public void 재고_조회_실패_테스트() {
        // given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.getStock(productId),
                "존재하지 않는 상품 조회 시 예외가 발생해야 합니다.");
        assertEquals("제품을 찾을 수 없습니다. productId=" + productId, ex.getMessage());
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void 단일상품조회_성공_테스트() {
        Long productId = 1L;
        Product product = new Product(
                productId,
                "Test Product",
                10_000,
                50,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        // findAllById 메서드 사용 (코드에 맞춰 작성)
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Product found = productService.getProductOrThrow(productId);
        assertNotNull(found);
        assertEquals(productId, found.id());
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void 단일상품조회_실패_테스트() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> productService.getProductOrThrow(productId));
        assertEquals("상품을 찾을 수 없습니다. productId=" + productId, e.getMessage());
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void 모든상품조회_테스트() {
        Product product1 = new Product(
                1L, "Product1", 10_000, 50, 1L,
                LocalDateTime.now(), LocalDateTime.now()
        );
        Product product2 = new Product(
                2L, "Product2", 20_000, 30, 2L,
                LocalDateTime.now(), LocalDateTime.now()
        );
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProducts();
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void 재고_차감_성공() {
        // given
        long productId = 1L;
        int initialStock = 50;
        int purchaseQuantity = 10;
        int expectedStock = initialStock - purchaseQuantity;

        // 초기 Product 도메인: 재고가 50
        Product product = new Product(productId, "Test Product", 10_000, initialStock, 1L, LocalDateTime.now(), LocalDateTime.now());
        // 실제 도메인 메서드 호출: 재고 차감 후 업데이트된 객체
        Product updatedProduct = product.decreaseStock(purchaseQuantity);

        // productRepository.findById(productId) 가 product를 반환
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        // productRepository.save(updatedProduct)를 호출하면 updatedProduct 반환
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // when
        Product result = productService.decreaseStock(productId, purchaseQuantity);

        // then: 재고(stock)가 purchaseQuantity 만큼 차감되어야 함
        assertEquals(expectedStock, result.stock());
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void 재고_증가_성공() {
        // given
        Long productId = 1L;
        int initialStock = 50;
        int restockQuantity = 20;
        int expectedStock = initialStock + restockQuantity;

        // 초기 Product 도메인: 재고가 50
        Product product = new Product(productId, "Test Product", 10_000, initialStock, 1L,  LocalDateTime.now(), LocalDateTime.now());
        // restock() 메서드 호출 후, 재고가 증가한 새 Product 인스턴스 생성
        Product updatedProduct = product.increaseStock(restockQuantity);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // when
        Product result = productService.increaseStock(productId, restockQuantity);

        // then: 재고(stock)가 restockQuantity 만큼 증가되어야 함
        assertEquals(expectedStock, result.stock());
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void 재고_차감_재고_부족_예외_테스트() {
        // given
        Long productId = 1L;
        int initialStock = 5;     // 재고가 부족한 상황 (예: 5)
        int purchaseQuantity = 10; // 구매하려는 수량 (예: 10)

        // 초기 Product 도메인: 재고가 5
        Product product = new Product(
                productId,
                "Test Product",
                10_000,
                initialStock,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // findById가 상품을 반환하도록 stub 설정
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> productService.decreaseStock(productId, purchaseQuantity));
        assertEquals("재고가 부족합니다.", e.getMessage());

        // 예외 발생 시 save는 호출되지 않아야 합니다.
        verify(productRepository, never()).save(any(Product.class));
    }

}