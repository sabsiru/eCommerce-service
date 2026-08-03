package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderLine;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
class PaymentCompletedConsumerTest {

    @Autowired
    private PaymentCompletedConsumer paymentCompletedConsumer;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PRODUCT_SALES_KEY = "product:sales:daily";
    private static final String PROCESSED_KEY_PREFIX = "payment:completed:processed:";

    @BeforeEach
    void setUp() {
        redisTemplate.delete(redisTemplate.keys(PRODUCT_SALES_KEY + "*"));
        redisTemplate.delete(redisTemplate.keys(PROCESSED_KEY_PREFIX + "*"));
    }

    @Test
    void 같은_결제완료_이벤트가_재전달돼도_인기상품_통계는_한번만_반영된다() {
        // given: 실제 Kafka 컨슈머가 자동으로 소비하기 전에, 같은 이벤트를 직접 두 번 전달해
        // 재전달(at-least-once) 상황을 재현한다.
        User user = userRepository.save(User.create("테스터", 100000));
        Product product = productRepository.save(new Product("상품1", 10000, 100, user.getId()));

        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 2, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);
        Payment payment = paymentRepository.save(Payment.withoutCoupon(order.getId(), order.getTotalAmount()));
        payment.complete();
        paymentRepository.save(payment);

        List<OrderItem> items = orderService.getOrderItems(order.getId());
        PaymentCompletedEvent event = new PaymentCompletedEvent(payment, order, items);

        Acknowledgment ack1 = mock(Acknowledgment.class);
        Acknowledgment ack2 = mock(Acknowledgment.class);

        // when: 동일 이벤트를 두 번 처리 (Kafka 재전달 재현)
        paymentCompletedConsumer.handlePaymentCompleted(event, ack1);
        paymentCompletedConsumer.handlePaymentCompleted(event, ack2);

        // then: 통계는 한 번만 반영되고, 재전달 건도 정상적으로 ack는 된다
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Double score = redisTemplate.opsForZSet().score(PRODUCT_SALES_KEY + ":" + today, product.getId().toString());
        assertThat(score).isEqualTo(2.0);

        verify(ack1).acknowledge();
        verify(ack2).acknowledge();
    }
}
