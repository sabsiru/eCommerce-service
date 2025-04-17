#!/bin/bash

echo "📦 K6 부하 테스트 시작..."

# k6 테스트 실행 + 결과를 JSON으로 저장 (성공 여부 무관)
k6 run ./k6-test-script.js --summary-export=./k6-result.json

echo "✅ 테스트 완료. 결과 요약 중..."

# 요약 리포트 생성 (Node.js 스크립트 실행)
node ./summarize.js

echo "📄 요약 리포트 생성 완료: ./k6-summary.md"