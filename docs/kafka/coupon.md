# 쿠폰 발급 기능 Kafka 리팩토링 설계 문서

## 1. 목표
- API 레이어(CouponFacade)에서는 요청 수락 및 Kafka 메시지 발행만 처리
- 실제 발급 로직(CouponService.issue)은 Kafka Consumer에서 실행
- 확장성, 내결함성, 운영 모니터링 강화

## 2. 비즈니스 시퀀스 다이어그램

~~~mermaid
sequenceDiagram
  actor User
  participant API as CouponFacade
  participant Broker as Kafka Broker
  participant Consumer as CouponIssuedConsumer
  participant Service as CouponService

  User ->> API: POST /{userId}/issue-async
  API -->> Broker: send(topic=inside.coupon.v1.issued, key=couponId, payload=CouponIssuedMessage)
  Broker ->> Consumer: deliver message
  Consumer ->> Service: issue(couponId, userId)
  Service -->> Redis: SADD issued:{couponId} userId + LPOP inventory
  Service -->> DB: 유저쿠폰 저장
  Service -->> Consumer: return void
  Consumer -->> Broker: commit offset
~~~

## 3. Kafka 구성
- **토픽**: `inside.coupon.v1.issued`
    - 파티션: `1` (동일 쿠폰 순차 처리 보장)
    - 복제 팩터: 운영 환경에 맞춰 설정
- **프로듀서**
    - Key Serializer: `StringSerializer`
    - Value Serializer: `JsonSerializer<CouponIssuedMessage>`
- **컨슈머**
    - Group ID: `coupon-issuer`
    - Key Deserializer: `StringDeserializer`
    - Value Deserializer: `JsonDeserializer<CouponIssuedMessage>`

## 4. 고려사항
- **순차성**: 키 기반 파티셔닝 + 파티션=1
- **동시성 제어**: Redis SADD를 이용한 한도·중복 체크(원자적) 및 DB 유니크 제약
- **장애 격리**: Consumer 장애 시 Kafka에 쌓인 메시지 복구 후 재처리
- **모니터링**: 토픽 오프셋, Consumer lag, 메시지 처리 성공/실패 메트릭
- **확장**: 별도 consumer 그룹 추가 및 토픽 파티션 수 조정 가능

## 5. 요약
Kafka 메시지 드리븐 아키텍처로 전환함으로써,
- API 응답 지연 없이 비동기 처리
- 이벤트 기반 서비스 연계
- 뛰어난 내결함성 및 확장성을 확보합니다.