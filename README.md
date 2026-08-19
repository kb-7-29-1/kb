<div align="center">
# 🛡️ 살고싶오

### 낯선 동네에서도, 안심하고 살 곳을 찾다

**주거 안전 + 귀갓길 안전 + 조건별 대출 매칭을 하나의 지도에서 해결하는 안심 주거 매칭 솔루션**

[![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Spring](https://img.shields.io/badge/Spring-Legacy%205-6DB33F?logo=spring&logoColor=white)](https://spring.io/)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/Team-29반%201팀-2E5AAC)]()

</div>
---

## 💡 왜 만들었나요

> "새로운 동네로 이사할 때, 이 골목이 안전한지 검색으로는 알 수 없었습니다."

1~2년 단기 계약이 잦은 **대학생·사회초년생·1인 가구**는 짧은 시간 안에 낯선 동네의 매물을 결정해야 합니다.
하지만 기존 부동산 플랫폼은 시세와 매물 정보만 보여줄 뿐, **"이 동네를 걸어 다녀도 안전한가?"**, **"이 건물은 법적으로 문제없나?"**, **"내 조건에 맞는 대출은 뭔가?"** 에는 답해주지 않습니다.

**살고싶오**는 이 세 가지 불안을 하나의 지도 위에서 동시에 해소합니다.

| 기존 서비스 | 살고싶오 |
|---|---|
| 매물 시세·구조 정보 위주 | **건물 안전 + 거리 안전 + 금융**을 통합 제공 |
| 안전 정보는 사용자가 직접 검색 | CCTV·가로등·파출소 기반 **CPTED 안전 점수 자동 산출** |
| 대출은 별도로 은행 앱에서 확인 | 매물 보증금·나이 조건에 맞는 **대출 상품 즉시 매칭** |
 
---

## ✨ 핵심 기능

| # | 기능                      | 설명 |
|---|-------------------------|---|
| 1 | 🎯 **초개인화 온보딩**         | 목적지(직장/학교) · 이동 수단 · 보증금/월세 예산 · 안전 선호도를 단계별 수집 |
| 2 | 🗺️ **지도 & 매물 탐색**      | 목적지 기준 도보권 시각화, 편의시설(편의점/카페/마트) 필터, 매물 마커 |
| 3 | 🏢 **건물 안전 정보**         | 위반건축물 여부, 준공연식 표시 |
| 4 | 🚨 **거리 안전 점수 (CPTED)** | CCTV·가로등 밀도, 파출소 접근성 기반 **0~100점 안전 점수** + 안심 귀갓길 경로 시각화 + 경로 만족도 투표 |
| 5 | 💬 **실거주 댓글 & 태그**      | 댓글 키워드를 자동 추출해 매물 특징 태그 배지 생성 |
| 6 | ⭐ **관심 매물(찜)**          | 찜하기/해제, 마이페이지 목록 관리 |
| 7 | 💰 **예산·나이 기반 금융 매칭**   | 예산·나이 조건 기반 대출 상품 추천, 은행 로고 표시, 클릭 시 은행 사이트 이동 |
| 8 | 👤 **회원 관리**            | 회원가입/로그인(JWT), 아이디·비밀번호 찾기, 프로필 수정, 회원 탈퇴 |
| 9 | 🔐 **세션 관리**            | 멀티탭 로그아웃 동기화, 세션 만료 임박 알림 및 연장 |
 
---

## 🧱 기술 스택

<table>
<tr>
<td valign="top" width="50%">
### 🎨 Frontend
`frontend/`

- **Vue 3** (Composition API, `<script setup>`)
- Vite · Pinia · Vue Router
- Axios · Tailwind CSS
- Naver Map API
</td>
<td valign="top" width="50%">
### ⚙️ Backend
`backend/`

- **Java 17** · Spring Framework 5 (Legacy, WAR) + Gretty(Tomcat 9)
- Spring Security + JWT (`jjwt`)
- MyBatis + MySQL + HikariCP
- 외부 연동: 국토교통부 공공데이터, 금감원 finlife(전세자금대출 상품 검색), Naver 지도/검색, 하이브리드 라우팅(Valhalla·MOTIS Docker + Tmap 자동 폴백)
</td>
</tr>
</table>
**공통/인프라**: Docker · GitHub 기반 협업 (main / develop / feature 브랜치 전략)
 
---

## 🏗️ 시스템 아키텍처

Vue 3 SPA가 Spring MVC(WAR)에 붙고, Tomcat이 정적 리소스와 API 요청을 분기합니다.
도보/대중교통 경로는 Valhalla·MOTIS(Docker) 하이브리드 엔진이 기본이며, 컨테이너가 죽으면 Tmap으로 자동 폴백합니다.

![시스템 아키텍처](architecture.png)

---

## 📁 프로젝트 구조

```
kb/
├── frontend/                 # Vue 3 + Vite 프론트엔드
│   └── src/
│       ├── api/               # Axios 인스턴스 및 도메인별 API 모듈
│       ├── components/        # 도메인별 재사용 컴포넌트
│       ├── composables/       # 재사용 가능한 Composition 함수
│       ├── pages/              # 라우트 단위 페이지
│       ├── router/             # Vue Router 설정
│       ├── stores/              # Pinia 전역 상태
│       └── utils/                # 포맷터, 은행 로고/링크 매칭 등
│
├── backend/                  # Spring Legacy + MyBatis 백엔드
│   └── src/main/java/com/salgosipo/
│       ├── global/              # 공통 설정, Security/JWT, 예외 처리
│       ├── auth/                 # 로그인, 아이디/비밀번호 찾기
│       ├── user/                  # 회원가입, 프로필, 탈퇴
│       ├── onboarding/             # 온보딩 조건 등록/조회
│       ├── destination/             # 목적지 검색/좌표 변환
│       ├── property/                 # 매물 정보 및 상세
│       ├── amenity/                   # 편의시설 필터
│       ├── comment/                    # 댓글 및 태그 자동 추출
│       ├── safety/                      # CPTED 안전 점수, 안심 경로, 투표
│       ├── bookmark/                     # 관심 매물(찜)
│       └── loan/                          # 나이·예산 조건 기반 대출 상품 추천
│
└── .agents/                  # 기획/설계 참고 문서 모음
```
 
---

## 🚀 시작하기

### 1. 환경 변수 설정

루트 또는 각 서비스에 `.env` 파일로 아래 키를 설정합니다 (`.env.local` 참고, 값은 별도 관리):

| 키 | 용도 |
|---|---|
| `VITE_NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | Naver 지도/로그인 |
| `NAVER_SEARCH_CLIENT_ID` / `NAVER_SEARCH_CLIENT_SECRET` | Naver 검색(목적지 검색) |
| `PUBLIC_DATA_SERVICE_KEY` | 공공데이터(건축물대장 등) |
| `SECURITY_LIGHT_API_KEY` | CCTV/가로등 등 안전 데이터 |
| `TMAP_API_KEY` | 보행자 경로 (Docker 라우팅 엔진 다운 시 자동 폴백) |
| `ODSAY_API_KEY` | 대중교통 경로 (예비 키, 현재 코드 미연동 — MOTIS/GraphHopper 사용 중) |
| `ROUTING_ENGINE_MODE` | `DOCKER`(기본) / `TMAP` — 라우팅 엔진 모드 |
| `ROUTING_VALHALLA_URL` / `ROUTING_MOTIS_URL` | 하이브리드 라우팅 엔진(Valhalla/MOTIS) 접속 URL |
| `OPENAI_API_KEY` | AI 관련 기능 |
| `LOAN_JEONSE_API_KEY` | 금감원 finlife 전세자금대출 상품 검색(`rentHouseLoanProductsSearch`) |

### 2. Frontend 실행

```bash
cd frontend
npm install
npm run dev       # 개발 서버 (Vite)
npm run build     # 프로덕션 빌드
```

### 3. Backend 실행

```bash
cd backend
./gradlew appRun   # Gretty로 Tomcat9 기동 (기본 포트 8080)
```

> DB 접속 정보 및 외부 API 키는 `backend/src/main/resources/application.properties`에서 설정합니다.
 
---

## 🗄️ 데이터베이스

MySQL 8.0+ · `utf8mb4` / `utf8mb4_unicode_ci` 기준

회원·목적지·온보딩·매물·이미지·댓글·태그·안전점수·안심경로·투표·편의시설·관심매물 등 12개 테이블로 구성됩니다.

![ERD](backend/src/main/resources/sql/kbfinal.png)

DDL은 [`backend/src/main/resources/sql/table.sql`](backend/src/main/resources/sql/table.sql), 컬럼별 설명은
[`.agents/db_schema_reference.md`](.agents/db_schema_reference.md)에서 확인할 수 있습니다 (ERD는 위 이미지가 최신 기준).
 
---

## 👥 팀 구성 (29반 1팀)

| 이름 | 담당 |
|---|---|
| **구혜성** (팀장/PM) | 회원관리, 관심 매물, 대출상품 추천 |
| **안효선** | 초개인화 온보딩(Step 1~5), 목적지 API, 온보딩 데이터 CRUD |
| **김민준** (PL) | 매물 카드 UI/리스트, 지도 렌더링, 커스텀 마커 |
| **김병승** | CPTED 안전 점수 알고리즘, Tmap 연동, 안심 귀갓길 시각화, 경로 투표 |
| **송은지** | 편의시설 커스텀 필터, 댓글 & 키워드 태그, 도보시간 차트 |
 
---

## 🌿 브랜치 전략 & 커밋 컨벤션

```
main (배포/제출용, PR로만 merge)
  └─ develop (통합 브랜치)
       └─ feature/{기능명}
```

- **커밋 타입**: `feat` `fix` `refactor` `docs` `style` `chore`
- 파일 10개 / 500줄 이상 대형 PR 지양 → 1개 기능 단위로 분할
- 팀원 최소 1명 승인 후 merge (**셀프 머지 금지**)
- 작업 시작 전 `git pull origin develop` **필수**
- 공통 설정 파일(`SecurityConfig`, `application.properties` 등) 수정 시 **팀 공지 후 진행**
---

## 📚 참고 문서 (`.agents/`)

| 문서 | 내용 |
|---|---|
| [agent.md](.agents/agent.md) | 프로젝트 마스터 허브 인덱스 |
| [scaffolding_guide.md](.agents/scaffolding_guide.md) | 프론트/백엔드 디렉토리 구조 및 초기 세팅 명령어 |
| [db_schema_reference.md](.agents/db_schema_reference.md) | 12개 테이블 컬럼 설명 (ERD는 `kbfinal.png` 참고) |
| [notion_summary_reference.md](.agents/notion_summary_reference.md) | 노션 정리 종합 가이드 & Git 컨벤션 |
| [wbs_reference.md](.agents/wbs_reference.md) | 8주차 마일스톤 및 과업 WBS |
| [e2e_test_guide.md](.agents/e2e_test_guide.md) | Playwright E2E 테스트 시나리오 |
| [mobile_ui_prompt.md](.agents/mobile_ui_prompt.md) / [pc_ui_prompt.md](.agents/pc_ui_prompt.md) | 모바일/PC UI 디자인 프롬프트 |
| [project_plan_prompt.md](.agents/project_plan_prompt.md) | 기획안 핵심 파트별 디자인 프롬프트 |
| [presentation_reference.md](.agents/presentation_reference.md) | 발표자료 요약 & CPTED 알고리즘 가이드 |
 
---

<div align="center">
**🏠 낯선 동네에서도, 살고 싶은 곳을 찾을 때까지 — 살고싶오**

</div>
</div>