# User 포인트 동시성 이슈 및 해결 보고서

## ✅ 문제 식별

### ⚙️ 시나리오
사용자가 동시에 포인트를 `충전`, `사용`, `환불`할 수 있는 환경에서 **데이터 정합성 문제**가 발생할 수 있습니다.
예를 들어, 포인트가 부족한데도 여러 요청이 거의 동시에 처리되면, 실제로는 부족한 포인트를 초과하여 사용하는 문제가 
발생할 수 있습니다.

---

## ✅ 분석

### 🔎 기존 구조 요약

도메인 객체인 `User`는 다음과 같은 로직을 가지고 있습니다:

```java
public void chargePoint(long amount) { /* 최대한도 검증 */ }
public void usePoint(long amount) { /* 포인트 잔액 검증 */ }
public void refundPoint(long amount) { /* 단순 적립 */ }
```

충전, 사용, 환불은 각각 다음 방식으로 처리됩니다:

```java
public User chargePoint(Long userId, int amount) {
    User updated = userPointService.chargePoint(userId, amount);
    pointHistoryService.saveCharge(userId, amount);
    return updated;
}
```
```java
public User usePoint(Long userId, int amount) {
// 포인트 사용 후 User 객체 반환
    User updated = userPointService.usePoint(userId, amount);
// 포인트 내역 저장
    pointHistoryService.saveUse(userId, amount);
    return updated;
}
```
```java
public User refundPoint(Long userId, int amount, Long orderId) {
    // 환불은 충전과 동일하게 처리
    User updated = userPointService.refundPoint(userId, amount);
    // 포인트 내역 저장
    pointHistoryService.saveRefund(userId, amount, orderId);
    return updated;
    }
```
하지만 이 로직은 **멀티스레드 환경에서의 데이터 정합성을 보장하지 않습니다**.

- 동일한 `User`가 1000 포인트를 보유한 상태에서,
- 10개의 스레드가 동시에 200포인트를 사용 요청하면,
- 총 2000포인트가 사용되는 상황이 발생할 수 있습니다 (Race Condition).

---

## ✅ 해결

### 🛠 동시성 테스트

**테스트 클래스**: `UserPointConcurrencyTest`

```java
@Test
void 동시_포인트_사용_정상_처리_확인() throws InterruptedException {
    User user = userRepository.save(User.create("동시성테스트", 1000));
    int threads = 10;
    int amount = 200;

    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    for (int i = 0; i < threads; i++) {
        executor.submit(() -> {
            try {
                userPointFacade.usePoint(user.getId(), amount);
            } catch (Exception e) {}
            finally { latch.countDown(); }
        });
    }

    latch.await();
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);

    User updated = userRepository.findById(user.getId()).orElseThrow();
    List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());

    assertThat(updated.getPoint()).isBetween(0, 1000);
    assertThat(histories.size()).isLessThanOrEqualTo(5); // 1000 / 200
}
```

위 테스트로 **실제 발생할 수 있는 동시성 이슈를 유도**합니다.

---
## ✅ 해결 방식 비교

| 구분 | 비관적 락 | 낙관적 락 |
|------|-----------|-----------|
| 동시성 제어 | 트랜잭션 시작 시 락 획득 | 커밋 시점에 충돌 검출 |
| 장점 | 정합성 매우 높음 | 성능 우수, 락 경합 없음 |
| 단점 | 성능 저하, 데드락 가능성 | 충돌 발생 시 재시도 로직 필요 |
| 적용 추천 | 포인트처럼 수치 정확성이 중요한 케이스 | 충돌 가능성이 낮은 대량 조회 등 |
---

### 💡 해결 방식: 비관적 락(Pessimistic Lock)

JPA의 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 또는 `FOR UPDATE` SQL을 활용하여 동시 접근을 차단합니다.

예시 (UserRepository):

---

## ✅ 판단 근거

`User`의 포인트는 금전적 가치와 직결되며,  
**한 번의 충돌이라도 심각한 정합성 문제로 이어질 수 있는 민감한 자원**입니다.  
실제로 동시성 충돌 빈도는 높지 않지만, 발생 시 데이터 오염 가능성이 큽니다.  
따라서 낙관적 락보다 **보다 보수적인 접근인 비관적 락을 적용하는 것이 적절**하다고 판단하였습니다.
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdForUpdate(@Param("id") Long id);
```

`UserPointService`에서 `findById()` 대신 `findByIdForUpdate()` 사용 시 동시 요청은 순차적으로 처리되므로 정합성이 보장됩니다.

---

## 🛠 UserPointService 리팩토링 비교

### 🔻 AS-IS

```java
public User usePoint(Long userId, int amount) {
    User user = userRepository.findById(userId)
    user.usePoint(amount);
    return userRepository.save(user);
}
public User getUserOrThrow(long userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저를 찾을수 없습니다."));
}
```

### 🔺 TO-BE

```java
@Transactional
public User usePoint(Long userId, int amount) {
    User user = userRepository.findByIdForUpdate(userId)
    user.usePoint(amount);
    return user;
}

public User getUserOrThrow(long userId) {
    return userRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저를 찾을수 없습니다."));
}
```

- `findById` → `findByIdForUpdate` 변경
- 트랜잭션 범위 내에서 포인트 수정 및 저장
- 충돌 발생 시 DB 레벨에서 선점 락 처리
---


## ✅ 결론

- `User 포인트 충전/사용/환불` 기능에서 동시성 이슈는 실제로 발생할 수 있음이 테스트를 통해 확인됨
- **비관적 락**을 통해 간단하고 확실하게 해결 가능

> [돌아가기](../../README.md)
