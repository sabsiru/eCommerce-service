package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderLine;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(
        properties = {
                "spring.kafka.consumer.group-id=payment-service",
                "spring.kafka.consumer.auto-offset-reset=earliest"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {"outside.payment.v1.completed"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PaymentKafkaTest {
    @Autowired
    KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    OrderService orderService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PaymentFacade paymentFacade;

    @Test
    void 카프카_결제_정상_흐름() {
        User user = userRepository.save(User.create("tester", 100000));
        Product product = productRepository.save(new Product("item", 30000, 5, 1L));
        List<OrderLine> lines = List.of(new OrderLine(product.getId(), 2, product.getPrice()));
        Order order = orderService.create(user.getId(), lines);

        Payment payment = paymentFacade.processPayment(order.getId(), order.getTotalAmount());

        assertThat(payment.getAmount()).isEqualTo(60000);
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
                    assertThat(updatedProduct.getStock()).isEqualTo(3);
                    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
                    assertThat(updatedUser.getPoint()).isEqualTo(100000 - 60000);
                });
    }
}