# Kafka 기본 개념 보고서

## 1. Kafka 개요
Kafka는 **대용량**과 **실시간 데이터 스트리밍**을 위해 설계된 분산 메시징 플랫폼입니다.
- **pub/sub 모델**을 사용
- **높은 처리량**과 **내결함성**을 보장
- 로그 구조(Log-structured) 저장소 기반

---

## 2. 아키텍처 구성 요소

| 구성 요소   | 역할                                                                        |
|----------|---------------------------------------------------------------------------|
| **Producer** | 메시지를 생성·전송하는 클라이언트. 토픽·파티션·키를 지정하여 `send(topic, key, value)` 호출. |
| **Broker**   | 메시지를 저장·관리하는 서버(클러스터). 토픽과 파티션을 호스팅하고, 복제·내구성을 제공.  |
| **Consumer** | 토픽으로부터 메시지를 구독·처리하는 클라이언트. Consumer Group 단위로 오프셋 관리.    |
| **Topic**    | 메시지를 논리적으로 구분하는 단위. 내부에 **Partition**을 가짐.                 |
| **Partition**| 물리적 분할 단위. 키 기반 파티셔닝으로 순서 보장 및 병렬 처리 가능.               |
| **Offset**   | 파티션 내 메시지의 순서 번호. Consumer는 오프셋을 커밋하여 재시작 시 위치 복원.      |
| **Cluster**  | 다수의 Broker가 모여 하나의 Kafka 클러스터를 형성. 고가용성과 스케일아웃 지원.   |

---

## 3. 핵심 개념

### 3.1 토픽과 파티션
- 토픽은 데이터 스트림의 이름표이며, 파티션은 그 안의 순서 저장소입니다.
- 프로듀서가 메시지를 보낼 때 키(key)를 지정하면, 해당 키 해시를 기반으로 특정 파티션에 할당되어 메시지 순서를 보장합니다.

### 3.2 오프셋(Offset)
- 각 파티션 내 메시지의 고유 일련번호입니다.
- 컨슈머는 마지막으로 처리한 오프셋을 커밋(commit)하여, 장애 복구 시 해당 지점부터 재처리하거나 건너뜁니다.

### 3.3 전달 의미(Delivery Semantics)
- **At-least-once**: 중복 가능성 있음(멱등성 처리 필요).
- **At-most-once**: 손실 가능성 있음.
- **Exactly-once**: 트랜잭션 기능(Streams API, Kafka Transactions) 활용해 중복 없이 정확히 한 번 전달.

### 3.4 Consumer Group
- 동일 그룹에 속한 컨슈머들이 파티션을 분산 소유하여 병렬 처리합니다.
- 그룹 내 한 컨슈머만 각 파티션을 읽어 순서 보장을 유지합니다.

### 3.5 스키마 관리
- JSON·Avro·Protobuf 등 메시지 포맷 관리.
- Schema Registry 도입 시, 호환성(Forward/Backward) 규칙을 정의해 생산자·소비자 버전 업그레이드를 안전하게 수행할 수 있습니다.

---

## 4. 메시지 흐름

1. **Producer**
  - 코드 예시:
    ```java
    // PaymentCompletedMessageHandler.send(...)
    kafkaTemplate.send(
      topic,                          // 토픽 이름 (application.yml: topic.payment-completed)
      event.getPayment().getId().toString(), // 메시지 키로 주문/결제 ID 사용
      event                           // 페이로드: PaymentCompletedEvent 객체
    );
    ```
  - **토픽 & 키** 지정 → Broker로 전송

2. **Broker**
  - `application.yml` 설정 예:
    ```yaml
    spring:
      kafka:
        bootstrap-servers: localhost:9092
    topic:
      payment-completed: outside.payment.v1.completed
    ```
  - **KafkaTemplate** 빈이 자동 생성되어 ProducerFactory와 함께 토픽에 메시지를 저장

3. **Consumer**
  - 코드 예시:
    ```java
    @Component
    public class PaymentCompletedEventConsumer {
      @KafkaListener(topics = "${topic.payment-completed}")
      public void handle(PaymentCompletedEvent event) {
        // 재고 차감·판매 집계 로직 호출
      }
    }
    ```
  - **@KafkaListener**를 통해 토픽 구독 → 메시지 처리

---

## 요약
Kafka는 분산 로그 기반 메시징 플랫폼으로,  
토픽 → 파티션 → 오프셋 구조를 통해 고성능·확장성을 제공합니다.  
Producer는 키 기반 파티셔닝으로 순서를 보장하며 메시지를 전송하고,  
Consumer Group을 통해 병렬 처리와 순서 보장을 조합합니다.  
At-least-once, At-most-once, Exactly-once 전달 의미와 스키마 관리가 안정적 스트리밍의 핵심입니다.