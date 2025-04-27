# 데이터베이스 락 개념 정리

## ✅ 용어 정리

| 용어 | 분류 | 설명 |
|------|------|------|
| **비관적 락 (Pessimistic Lock)** | 전략 | 충돌이 발생할 것으로 예상하고, 트랜잭션 초반에 DB에 락을 걸어 다른 트랜잭션의 접근을 막는 방식 |
| **낙관적 락 (Optimistic Lock)** | 전략 | 충돌 가능성을 낮게 보고 락 없이 처리한 뒤, 커밋 시점에 충돌 여부를 검증하는 방식 |
| **S-Lock (Shared Lock)** | 물리적 락 | 동시에 여러 트랜잭션이 읽을 수 있으나, 쓰기는 불가능한 락 (공유 잠금) |
| **X-Lock (Exclusive Lock)** | 물리적 락 | 읽기/쓰기 모두 막는 락. 하나의 트랜잭션만 접근 가능 (배타 잠금) |

---

## 💡 관계도

- **비관적 락 (Pessimistic Lock)** 은 DB의 **S-Lock / X-Lock**과 같은 **물리적 락을 내부적으로 사용**합니다.
    - 예시: `SELECT ... FOR UPDATE` → 해당 row에 **X-Lock** 설정
- **낙관적 락 (Optimistic Lock)** 은 DB 락을 사용하지 않고, 애플리케이션 차원에서 **@Version 필드 등으로 충돌을 감지**합니다.

```text
비관적 락 (Pessimistic Lock)
├─ S-Lock (읽기만 허용)
└─ X-Lock (읽기/쓰기 모두 차단)

낙관적 락 (Optimistic Lock)
└─ DB 락 없음, 커밋 시점에 버전 충돌 확인 (@Version)
```

---

## 🔁 비유로 이해하기

| 전략 | 상황 비유 | 설명 |
|------|-----------|------|
| **비관적 락** | 은행 ATM 줄서기 | 한 명씩만 들어가서 처리를 끝내고 나와야 다른 사람이 접근 가능 |
| **낙관적 락** | 공동 문서 편집 | 여러 명이 동시에 작업하고, 저장할 때 충돌이 발생하면 저장 실패 후 재시도 |

---

## 🔐 사용 예시 (JPA 기반)

### 📌 비관적 락

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdForUpdate(@Param("id") Long id);
```

### 📌 낙관적 락

```java
@Version
private Long version;
```

```java
// 저장 시 버전 번호를 비교해서 충돌 여부 판단
```

---

## 📌 정리

- **정합성이 중요한 상황** (ex: 포인트, 재고 등)에서는 **비관적 락**이 선호됩니다.
- **낮은 충돌 확률**과 **성능 우선 상황**에서는 **낙관적 락**이 적합합니다.

> ⚠️ 락 사용 시, 데드락 방지 및 트랜잭션 범위 최소화에 유의해야 합니다.
> 
> [돌아가기](../../README.md)