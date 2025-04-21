package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    public void 재고_단순조회_정상_테스트() {
        // given
        Long productId = 1L;
        int expectedStock = 50;

        Product product = new Product(productId, "Test Product", 10000, expectedStock, 1L, LocalDateTime.now(), LocalDateTime.now());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        int actualStock = productService.getStock(productId);

        // then
        assertEquals(expectedStock, actualStock);
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void 재고확인_성공() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .id(productId)
                .name("테스트 상품")
                .price(5000)
                .stock(10)
                .categoryId(1L)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when & then
        assertDoesNotThrow(() -> productService.checkStock(productId, 5));
    }

    @Test
    void 재고확인_실패_재고부족() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .id(productId)
                .name("테스트 상품")
                .price(5000)
                .stock(3)
                .categoryId(1L)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productService.checkStock(productId, 5));

        assertEquals("상품 재고가 부족합니다.", exception.getMessage());
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

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Product found = productService.getProductOrThrow(productId);
        assertNotNull(found);
        assertEquals(productId, found.getId());
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

        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> result = productService.getProducts(pageable);

        verify(productRepository, times(1)).findAll();
    }

    @Test
    void 재고_차감_성공() {
        // given
        long productId = 1L;
        int initialStock = 50;
        int purchaseQuantity = 10;
        int expectedStock = initialStock - purchaseQuantity;

        Product product = new Product(productId, "Test Product", 10_000, initialStock, 1L, LocalDateTime.now(), LocalDateTime.now());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // when
        Product result = productService.decreaseStock(productId, purchaseQuantity);

        // then
        assertEquals(expectedStock, result.getStock());
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

        Product product = new Product(productId, "Test Product", 10_000, initialStock, 1L, LocalDateTime.now(), LocalDateTime.now());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // when
        Product result = productService.increaseStock(productId, restockQuantity);

        // then
        assertEquals(expectedStock, result.getStock());
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void 재고_차감_재고_부족_예외_테스트() {
        // given
        Long productId = 1L;
        int initialStock = 5;
        int purchaseQuantity = 10;

        Product product = new Product(
                productId,
                "Test Product",
                10_000,
                initialStock,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> productService.decreaseStock(productId, purchaseQuantity));
        assertEquals("재고가 부족합니다.", e.getMessage());

        verify(productRepository, never()).save(any(Product.class));
    }

}