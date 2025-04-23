# 결제,환불 및 재고 동시성 이슈 및 해결 보고서

## ✅ 문제 식별

### ⚙️ 시나리오
1. **동일 주문에 동시 결제 요청**
   - 같은 `orderId`로 `processPayment` 동시 호출 → 중복 결제 레코드 생성
2. **동일 결제에 동시 환불 요청**
   - 같은 `paymentId`로 `processRefund` 동시 호출 → 중복 환불, 포인트·재고 과다 복원
3. **재고 차감/증가 시 동시 접근**
   - 재고가 음수로 감소하거나, 환불 후 과도하게 증가

---

## ✅ 분석

### 🔎 기존 구조 요약

```java
public Payment processPayment(Long orderId, int amount) {
    Order order = orderService.getOrderOrThrow(orderId); // 잠금×
    productService.decreaseStock(...);                   // 잠금×
    orderService.pay(orderId);                           // 잠금×
    paymentService.initiate(...);                        // INSERT만
    paymentService.completePayment(...);                 // 상태 변경만
    return payment;
}
```

- 주문·재고·결제 모두 락 없이 처리 → race condition
- 환불 흐름도 락/검증 분리 → 재고 복원 누락 또는 중복 실행

### 🛠 동시성 테스트 예시

```java
@Test
void 재고_1개_상품_동시_차감_테스트() throws InterruptedException {
    Product product = productRepository.save(new Product("한정판", 10000, 1, 1L));
    Order order = orderService.create(user.getId(), List.of(
        new OrderLine(product.getId(), 1, product.getPrice())
    ));

    int threads = 5;
    CountDownLatch latch = new CountDownLatch(threads);
    ExecutorService exec = Executors.newFixedThreadPool(threads);
    for (int i = 0; i < threads; i++) {
        exec.submit(() -> {
            try {
                paymentFacade.processPayment(order.getId(), order.getTotalAmount());
            } catch (Exception ignored) {}
            finally { latch.countDown(); }
        });
    }
    latch.await();

    Product updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStock()).isZero();
}
```

---

## ✅ 해결

### Pessimistic Lock 적용

```java
// OrderRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findByIdForUpdate(@Param("id") Long id);

// PaymentRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Payment p WHERE p.id = :id")
Optional<Payment> findByIdForUpdate(@Param("id") Long id);

// ProductRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdForUpdate(@Param("id") Long id);
```

1. **결제 생성**
```java
// OrderService
public Order pay(Long orderId) {
Order order = getOrderOrThrowPaid(orderId);
order.pay();
return save(order);
}
public Order getOrderOrThrowPaid(Long orderId) {
Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(...);
if (order.getStatus() == OrderStatus.PAID) throw ...;
return order;
}
```

2. **환불 처리**
```java
// PaymentService
public Payment refundPayment(Long paymentId) {
Payment payment = getPaymentForRefundOrThrow(paymentId);
payment.refund();
return paymentRepository.save(payment);
}
public Payment getPaymentForRefundOrThrow(Long paymentId) {
Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow(...);
if (payment.getStatus() == PaymentStatus.REFUND) throw ...;
return payment;
}
   ```

3. **재고 제어**
```java
// ProductService
public Product decreaseStock(long productId, int decreaseQuantity) {
Product product = getProductOrThrow(productId);
product.decreaseStock(decreaseQuantity);
return productRepository.save(product);
}
public Product increaseStock(long productId, int increaseQuantity) {
Product product = getProductOrThrow(productId);
product.increaseStock(increaseQuantity);
return productRepository.save(product);
}
   ```

---

## ✅ 해결 방식 비교

| 대상             | 비관적 락 (PESSIMISTIC_WRITE)            | 낙관적 락 (@Version) + 재시도               |
|----------------|---------------------------------------|----------------------------------------|
| **결제/환불**      | 행 잠금 → 완전 직렬 처리<br>충돌 시 대기         | 버전 검사 → OptimisticLockException 발생 + 재시도 |
| **재고 차감/증가** | FOR UPDATE → oversell/over-refund 방지        | 버전 검사 → 재시도<br>충돌 시 코드 복잡도 증가   |
| **장점**          | 보장된 정합성, 구현 및 검증 로직 단순             | 락 경합 없음, 평상시 성능 우수              |
| **단점**          | 트래픽 급증 시 대기/데드락 우려                 | 인기 자원에선 재시도 폭증, 예외 처리 필요     |

---

## 💡 판단 근거

- 주문·결제·재고는 **한정 자원**이기에 충돌 시 즉시 직렬화가 필요함
- 재고 관리처럼 충돌 빈도가 낮은 영역만 낙관적 락을 제한적으로 고려
- PESSIMISTIC_WRITE 기반으로 동시성 이슈(중복 결제·환불·재고 불일치) 완전 해결 가능

---

## 🛠 리팩토링 비교 (AS-IS / TO-BE)

### AS-IS

```java
public User getOrderOrThrow(Long orderId) {         // 조회만
    return orderRepo.findById(orderId).orElseThrow();
}
public Product decreaseStock(...) {                 // 조회만
    Product p = productRepo.findById(productId).orElseThrow();
    p.decreaseStock(qty);
    return productRepo.save(p);
}
public Payment refundPayment(...) {                  // 상태만 변경
    Payment p = paymentRepo.findById(paymentId).orElseThrow();
    p.refund();
    return paymentRepo.save(p);
}
```

### TO-BE

```java
public Order getOrderForPayOrThrow(Long orderId) {   // FOR UPDATE 잠금 + 검증
    Order o = orderRepo.findByIdForUpdate(orderId).orElseThrow();
    if (o.getStatus() != PENDING) throw ...;
    return o;
}

public Payment getPaymentForRefundOrThrow(Long paymentId) { // FOR UPDATE + 검증
    Payment p = paymentRepo.findByIdForUpdate(paymentId).orElseThrow();
    if (p.getStatus() != COMPLETED) throw ...;
    return p;
}

public void decreaseStock(Long productId, int qty) {     // FOR UPDATE + 변경
    Product p = productRepo.findByIdForUpdate(productId).orElseThrow();
    p.decreaseStock(qty);
}
```

---

## ✅ 결론

- **행 수준 FOR UPDATE 잠금**으로 주문·결제·재고 처리 흐름을 하나의 트랜잭션 안에 묶어 직렬화 보장
- 충돌 빈도가 낮은 비핵심 영역만 낙관적 락 검토
- 위 전략으로 모든 동시성 이슈를 명확히 해결할 수 있습니다.


> [돌아가기](../../README.md)