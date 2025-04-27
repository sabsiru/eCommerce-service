# Coupon 동시성 이슈 및 해결 보고서

## ✅ 문제 식별

### ⚙️ 시나리오
1. 동일 쿠폰을 여러 사용자가 동시에 발급 요청 시, **발급 수량 제한**을 초과하는 문제가 발생
2. 동일 사용자가 동일 쿠폰을 동시에 발급 요청할 경우, **중복 발급**이 발생

---

## ✅ 분석

### 🔎 기존 구조 요약

```java
public UserCoupon issueCoupon(Long userId, Long couponId) {
    Optional<UserCoupon> existing = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
    if (existing.isPresent()) {
        throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
    }

    Coupon coupon = getCouponOrThrow(couponId);
    Coupon updated = coupon.increaseIssuedCount();
    couponRepository.save(updated);

    UserCoupon userCoupon = UserCoupon.issue(userId, coupon.getId());
    return userCouponRepository.save(userCoupon);
}
```

- 조회→증가→저장 순으로 처리되며, 동시 요청 시 race condition 발생

### 🛠 동시성 테스트 예시

```java
@Test
void 쿠폰_동시_발급_테스트() throws InterruptedException {
    Coupon coupon = couponRepository.save(new Coupon("할인쿠폰", 1000, 1));
    int threads = 10;
    CountDownLatch latch = new CountDownLatch(threads);
    ExecutorService exec = Executors.newFixedThreadPool(threads);

    for (int i = 0; i < threads; i++) {
        long userId = userRepository.save(User.create("user"+i, 0)).getId();
        exec.submit(() -> {
            try {
                couponService.issueCoupon(userId, coupon.getId());
            } catch (Exception ignored) {}
            finally { latch.countDown(); }
        });
    }
    latch.await();

    long issued = userCouponRepository.countByCouponId(coupon.getId());
    assertThat(issued).isEqualTo(1);
}
```

---

## ✅ 해결 방식 비교

| 구분             | 비관적 락 (PESSIMISTIC_WRITE)            | 낙관적 락 (@Version) + 재시도       |
|----------------|---------------------------------------|----------------------------------|
| **수량 초과 제어**  | `SELECT ... FOR UPDATE` → 순차 처리          | 버전 검사 → 예외 + 재시도 필요         |
| **중복 발급 제어**  | DB Unique 제약 + 예외 처리                 | 조회 후 예외 처리, 재시도 복잡도 증가  |
| **장점**          | 간단·확실한 정합성 보장                   | 락 경합 없음, 평상시 성능 우수         |
| **단점**          | 락 대기, 트래픽 급증 시 지연 가능            | 충돌 시 재시도 폭증, 구현 복잡도 증가  |

---

### 💡 해결 방식: 비관적 락(Pessimistic Lock)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Coupon c WHERE c.id = :id")
Optional<Coupon> findByIdForUpdate(Long id);
```

- `issueCoupon` 내 `findByIdForUpdate(couponId)` 호출 후 `increaseIssuedCount()` 실행

---

## 🛠 리팩토링 비교 (AS-IS / TO-BE)

### 🔻 AS-IS

```java
public UserCoupon issueCoupon(Long userId, Long couponId) {
    Optional<UserCoupon> existing = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
    // …기존 로직 그대로…
}
```

### 🔺 TO-BE

```java
@Transactional
public UserCoupon issueCoupon(Long userId, Long couponId) {
    Coupon coupon = couponRepository.findByIdForUpdate(couponId)
        .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
    Coupon updated = coupon.increaseIssuedCount();
    couponRepository.save(updated);

    try {
        return userCouponRepository.save(UserCoupon.issue(userId, couponId));
    } catch (DataIntegrityViolationException e) {
        throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
    }
}
```

---

## ✅ 결론

- `비관적 락 + DB Unique 제약`의 조합으로 **발급 수량 초과** 및 **중복 발급** 이슈 해결
- 테스트 환경에서 재현된 race condition을 효과적으로 방지

> [돌아가기](../../README.md)