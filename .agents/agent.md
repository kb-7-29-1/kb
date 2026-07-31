# 🤖 [살고싶오] 개발 총괄 가이드 & 마스터 허브 (agent.md)

본 문서는 **살고싶오(29반 1팀) - 낯선 지역 이주 시 주거 및 골목 귀갓길 안전 불안을 해소하기 위한, 안심 주거 매칭 & 개인화 금융 솔루션** 프로젝트의 **중앙 제어 마스터 허브(Master Hub Index)** 문서입니다.
AI 개발 도구 및 팀원은 핵심 요약 흐름을 본 문서에서 확인하고, 항목별 구체적인 스펙 및 DDL/프롬프트 코드는 **아래 세부 연동 문서 링크**를 참조합니다.

---

## 📌 0. 프로젝트 핵심 정체성 (Single Source of Truth)

- **프로젝트 명**: 살고싶오 (29반 1팀)
- **주제**: 낯선 동네의 주거 및 귀갓길 안전 불안을 해소하는 안심 주거 매칭 플랫폼 ('건물 안전' + '거리 안전' + '개인화 금융 대출 매칭')
- **핵심 타깃**: 1~2년 단기 계약 대학생, 사회초년생, 1인 가구, 낯선 지역 이주자
- **문서 탐색 체계**: `1. 스캐폴딩` ➔ `2. UI/UX 디자인` ➔ `3. WBS 일정 & R&R` ➔ `4. 개발 스펙 & DB 스키마` ➔ `5. E2E 통합 테스트`

---

## 🏗️ 1. 프로젝트 뼈대 & 스캐폴딩 허브 (Scaffolding Hub)

### ■ 핵심 아키텍처 구조 요약

- **Frontend (`salgosipo-fe/`)**: Vue 3 (Vite + Composition API), Pinia, Vue Router v4, Tailwind CSS, Axios Client
- **Backend (`salgosipo-be/`)**: Java 17, Spring, Security (JWT), MyBatis (XML Mapper), MySQL, Swagger
- **Test (`tests/e2e/`)**: Playwright E2E Test Suite

### 🔗 상세 디렉토리 구조 & 스캐폴딩 명령어 문서 (Click to View)

- 🏗️ [프로젝트 뼈대 & 스캐폴딩 가이드 (scaffolding_guide.md)](scaffolding_guide.md)

---

## 🎨 2. UI/UX 디자인 & 레이아웃 허브 (UI/UX Hub)

### ■ 핵심 디자인 원칙 요약

- **Design Tokens**: Primary KB Blue (`#2A60F7`), Safety Palette (🟢 80~100 / 🟡 60~79 / 🔴 0~59), Glassmorphism, Radius 20px
- **Layout Architecture**:
  - Mobile (iPhone 17 Pro Max - 440 x 956px, 6.9인치 19.6:9, 1320x2868px): Bottom Sheet, Floating Controls, Mobile First
  - PC Widescreen (1440px~1920px): Left 380px Sidebar + Center Map Canvas + Right 560px Sliding Door Detail Panel + 3-Column Wide Table

### 🔗 상세 UI/UX 비주얼 디자인 프롬프트 문서 (Click to View)

- 📱 [모바일 전 화면(20개) 1:1 디자인 프롬프트 (mobile_ui_prompt.md)](mobile_ui_prompt.md)
- 🖥️ [PC 웹 전 화면 와이드 뷰 디자인 프롬프트 (pc_ui_prompt.md)](pc_ui_prompt.md)
- 📋 [기획안 5대 핵심 파트별 디자인 프롬프트 (project_plan_prompt.md)](project_plan_prompt.md)

---

## 📅 3. WBS(작업 분해 구조도) & R&R 역할분담 허브 (WBS Hub)

### ■ 팀원별 R&R 요약

- **구혜성 (팀장/PM)**: 로그인/회원가입, 관심 매물, 마이페이지 프로필/탈퇴, `agent.md`
- **안효선**: 초개인화 온보딩 폼 (Step 1~5), 목적지 API, 온보딩 데이터 CRUD, 온보딩 요약 패널
- **김민준 (PL)**: 매물 카드 UI & 리스트 렌더링/5종 정렬, 지도 렌더링, 커스텀 마커, 매물 기본 정보 패널
- **김병승**: CPTED V1 감산 안전 점수 알고리즘 (100점 만점), Tmap 보행자 API 연동, 안전 귀갓길 Glowing Line 시각화, 경로 투표 모달
- **송은지**: 편의시설 7종 커스텀 필터, 실거주 댓글 & 키워드 태그 뱃지, 주변 편의시설 도보시간 차트바 패널

### 🔗 상세 WBS 및 8주차 일정 문서 (Click to View)

- 📅 [8주차 마일스톤 & 108개 과업 상세 WBS 문서 (wbs_reference.md)](wbs_reference.md)

---

## 📝 4. 개발 스펙, DB 스키마 & 알고리즘 허브 (Tech & DB Hub)

### ■ 핵심 개발 스펙 요약

- **Tech Stack**: Vue 3 (Composition API `<script setup lang="ts">`), Pinia, Vue Router v4 / Java 17, Spring, MyBatis, MySQL
- **Hybrid API**: Naver Local API (편의시설) + Tmap 보행자 API (LineString) + ODsay 대중교통 API 하이브리드 파이프라인
- **CPTED V1 감산 알고리즘**: $S = \operatorname{Max}(0, 100 - P_{\text{CCTV\_밀도}} - P_{\text{CCTV\_분포}} - P_{\text{조명\_분포}} - P_{\text{파출소}})$ (동선 반경 30~50m 집계)

### 🔗 상세 DB 스키마 DDL, ERD 및 발표자료 정리 문서 (Click to View)

- 🗄️ [11개 테이블 완전한 MySQL DDL 스크립트 & ERD (db_schema_reference.md)](db_schema_reference.md)
- 📝 [노션 정리 종합 개발 가이드 & Git 규칙 (notion_summary_reference.md)](notion_summary_reference.md)
- 📊 [발표자료 57페이지 종합 요약 & CPTED 알고리즘 가이드 (presentation_reference.md)](presentation_reference.md)

---

## 🧪 5. E2E 전용 통합 테스트 가이드 허브 (E2E Test Hub)

### ■ 핵심 E2E 시나리오 테스트 매트릭스 요약

- **TC-01 [인증]**: 회원가입 ➔ 로그인 ➔ JWT 토큰 수령
- **TC-02 [온보딩]**: 목적지 검색 ➔ 대중교통/도보 ➔ 듀얼 예산 슬라이더 ➔ 안전 가중치 ➔ DB 저장
- **TC-03 [지도&필터]**: 15분 이소크론 반원 영역 ➔ 편의시설 7종 도보 제한 필터 ➔ 마커 뱃지 (`4/5`) 업데이트
- **TC-04 [매물상세]**: 우측 560px 슬라이딩 도어 ➔ 위반건축물 OX 뱃지 ➔ 5축 레이더 차트 ➔ 안심 귀갓길 Glowing Line ➔ 댓글 & 귀갓길 평가 모달
- **TC-05 [마이페이지]**: 내 조건 수정 ➔ 찜한 매물 3열 수평 비교 테이블 렌더링

### 🔗 상세 E2E 테스트 시나리오 및 Playwright 자동화 코드 문서 (Click to View)

- 🧪 [Playwright 기반 E2E 시나리오 테스트 가이드 (e2e_test_guide.md)](e2e_test_guide.md)
