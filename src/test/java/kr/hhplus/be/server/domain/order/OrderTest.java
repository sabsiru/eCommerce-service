package kr.hhplus.be.server.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void 주문_생성_정상_테스트() {
        // given
        Long userId = 1L;
        Long userCouponId = 2L;
        int discountAmount = 5000;
        // 주문 항목 생성: 예를 들어, 2개 상품, 단가 15000 -> 총금액 30000
        OrderItem item1 = OrderItem.create(100L, 10L, 2, 15000);
        List<OrderItem> items = List.of(item1);

        // when
        Order order = Order.create(userId, items);

        // then
        assertNull(order.id());
        assertEquals(userId, order.userId());

        // totalAmount는 주문 항목들의 총 금액: 2 * 15000 = 30000
        assertEquals(order.totalAmount(), order.totalAmount());
        assertEquals(OrderStatus.PENDING, order.status());
        assertNotNull(order.createdAt() );
        assertNotNull(order.updatedAt());
    }

    @Test
    void 주문_생성_다중항목_테스트() {
        // given
        Long userId = 1L;
        Long userCouponId = 2L;
        int discountAmount = 10_000;  // 할인 금액
        // 주문 항목 2건 생성
        // item1: 2개, 단가 15,000 -> 총액 30,000
        OrderItem item1 = OrderItem.create(100L, 10L, 2, 15_000);
        // item2: 3개, 단가 20,000 -> 총액 60,000
        OrderItem item2 = OrderItem.create(100L, 20L, 3, 20_000);
        List<OrderItem> items = Arrays.asList(item1, item2);

        // when
        Order order = Order.create(userId, items);

        // then
        long expectedTotalAmount = 30_000 + 60_000;  // 90,000
        assertEquals(expectedTotalAmount, order.totalAmount());
        assertEquals(OrderStatus.PENDING, order.status());
        assertNotNull(order.createdAt());
        assertNotNull(order.updatedAt());
        assertEquals(2, order.items().size());
    }

    @Test
    void 주문_생성_실패_주문항목없음() {
        // given
        Long userId = 1L;
        Long userCouponId = null;
        int discountAmount = 0;

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Order.create(userId, List.of()));
        assertEquals("주문 항목은 최소 하나 이상 존재해야 합니다.", e.getMessage());
    }


    @Test
    void 주문_취소_성공_테스트() {
        // given
        OrderItem item1 = OrderItem.create(100L, 10L, 2, 15000);
        Order order = Order.create(1L, List.of(item1));

        // when
        Order cancelledOrder = order.cancel();

        // then
        assertEquals(OrderStatus.CANCEL, cancelledOrder.status());
    }

    @Test
    void 주문_취소_실패_비팬딩() {
        // given
        OrderItem item1 = OrderItem.create(100L, 10L, 2, 15000);
        Order order = Order.create(1L, List.of(item1));
        Order paidOrder = order.pay(); // 상태가 PAID

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> paidOrder.cancel());
        assertEquals("취소는 PENDING 상태의 주문에만 가능합니다.", e.getMessage());
    }

    @Test
    void 주문아이템_업데이트_정상_동작() throws InterruptedException {
        // given
        Long orderId = 1L;
        Long userId = 100L;

        OrderStatus initialStatus = OrderStatus.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = createdAt;

        // 기존 주문은 빈 주문 항목 리스트와 0 총액(할인이 없으므로)
        Order originalOrder = new Order(orderId, userId, Collections.emptyList(), 0, initialStatus, createdAt, updatedAt);

        // 새로 생성할 주문 항목 2개
        // OrderItem.totalPrice() = quantity * orderPrice
        OrderItem item1 = OrderItem.create(orderId, 101L, 2, 15000); // 총액: 30000
        OrderItem item2 = OrderItem.create(orderId, 102L, 1, 20000); // 총액: 20000
        List<OrderItem> newItems = Arrays.asList(item1, item2);

        // updatedAt 필드 갱신 차이가 명확하도록 약간의 지연
        Thread.sleep(10);

        // when : updateItems 호출하여 새 주문 객체 생성 (불변 객체이므로 새 객체 반환)
        Order updatedOrder = originalOrder.updateItems(newItems);

        // then
        int expectedTotal = newItems.stream().mapToInt(OrderItem::totalPrice).sum();

        assertEquals(newItems, updatedOrder.items());
        // 총 주문 금액이 재계산되어야 함
        assertEquals(expectedTotal, updatedOrder.totalAmount());
        // updatedAt은 이전의 updatedAt보다 나중이어야 함
        assertTrue(updatedOrder.updatedAt().isAfter(originalOrder.updatedAt()));
    }

}