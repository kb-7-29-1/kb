# Docker 기반 하이브리드 라우팅(Valhalla·MOTIS) 구조

> Tmap API 호출 제한 문제 때문에, 도보/대중교통 경로 연산을 Docker로 띄운 오픈소스 라우팅 엔진(Valhalla·MOTIS)으로 전환했다. 이 문서는 그 배경과 Docker 동작 원리를 정리한 것.

## 관련 코드 위치

- `backend/src/main/java/com/salgosipo/global/routing/RoutingEngineRouter.java` — Valhalla 실패 시 Tmap으로 자동 폴백하는 스위처
- `backend/src/main/java/com/salgosipo/global/routing/ValhallaPedestrianClient.java` — Valhalla 보행자 경로 클라이언트
- `backend/src/main/java/com/salgosipo/global/routing/MotisTransitClient.java` — MOTIS 대중교통 클라이언트
- `backend/src/main/java/com/salgosipo/global/routing/HybridRoutingService.java`
- `backend/src/main/resources/docker-routing.properties` — 엔진 모드/URL 설정
- 루트 `Dockerfile` — (Valhalla/MOTIS와 별개로) 이 프로젝트 자체(Vue+Spring)를 빌드하는 멀티스테이지 Dockerfile

## 1. 왜 Tmap 대신 Docker인가

Tmap은 외부 API라 **호출 횟수만큼 과금/제한**이 걸린다. Valhalla(보행자 경로)와 MOTIS(대중교통)는 **오픈소스 라우팅 엔진**이라 직접 서버에 띄우면 호출 제한이 없다. 문제는 이 엔진들이:

- OSM(OpenStreetMap) 지도 데이터 + 전처리된 타일/그래프 파일
- 특정 버전의 C++ 런타임, 라이브러리 의존성
- 복잡한 빌드 과정

을 필요로 해서 로컬에 "그냥 설치"하기 번거롭고, 팀원마다 환경이 달라지면 "내 컴퓨터에서는 되는데" 문제가 생긴다. Docker는 이 문제를 해결한다.

## 2. 컨테이너 vs VM — 핵심 원리

VM은 하드웨어를 통째로 가상화해서 OS 커널까지 새로 띄운다(무겁고 느림). **컨테이너는 호스트 OS 커널을 그대로 공유**하고, 프로세스를 파일시스템/네트워크/자원 관점에서만 격리한다. 그래서:

- Valhalla 컨테이너 안에는 Valhalla 실행에 필요한 라이브러리 + OSM 데이터만 들어있는 "격리된 파일시스템"이 있음
- 호스트 커널을 그대로 빌려 쓰기 때문에 VM보다 훨씬 가볍고 빠르게 뜸
- 컨테이너를 지워도 호스트 시스템에는 아무 영향 없음

## 3. 이미지(Image) → 컨테이너(Container)

- **이미지**: 실행에 필요한 모든 것(OS 레이어 + 바이너리 + 설정 + 데이터)이 압축된 "설계도/스냅샷". 읽기 전용.
- **컨테이너**: 이미지를 실제로 실행한 "살아있는 인스턴스". 하나의 이미지로 여러 컨테이너를 띄울 수 있음.

Valhalla/MOTIS는 Docker Hub의 공식 이미지를 pull 받아서 그대로 실행하는 방식:

```bash
docker pull valhalla/valhalla:latest
docker run -d -p 8000:8002 -v $(pwd)/valhalla_data:/data valhalla/valhalla:latest
```

Valhalla 팀이 이미 "Valhalla가 돌아가는 완벽한 환경"을 이미지로 패키징해뒀기 때문에, 의존성 설치 걱정 없이 `docker run` 한 줄로 바로 쓸 수 있다.

반면 프로젝트 루트 `Dockerfile`은 **직접 이미지를 빌드**하는 예시다(멀티스테이지 빌드):

```
frontend-builder (node:20-alpine) → npm run build
   ↓ (빌드 결과물만 복사)
backend-builder (gradle:8.8-jdk17) → gradle build
   ↓ (war 파일만 복사)
tomcat:9.0-jdk17-temurin (최종 실행 이미지)
```

각 단계가 하나의 "레이어"고, 최종 이미지에는 각 단계에서 필요한 산출물만 남는다(빌드 도구는 안 남음 → 이미지 크기 절약).

## 4. 왜 `localhost:8000`, `localhost:8001`로 접근되는가

`docker run -p 8000:8002 ...`에서 `-p 호스트포트:컨테이너포트`가 핵심이다. 컨테이너는 자체 네트워크 네임스페이스를 갖고 있어서(예: 컨테이너 내부는 8002 포트로 리슨) 기본적으로는 외부에서 안 보인다. `-p` 옵션이 호스트의 8000번 포트를 컨테이너의 8002번 포트로 **포트포워딩**해줘서, Spring 백엔드가 `http://localhost:8000`으로 호출하면 실제로는 Valhalla 컨테이너 내부 프로세스로 전달된다.

`docker-routing.properties`에 `ROUTING_VALHALLA_URL` / `ROUTING_MOTIS_URL` 환경변수를 오버라이드할 수 있게 해둔 이유도, 배포 환경(Railway)에서는 `localhost`가 아니라 다른 호스트 주소를 써야 하기 때문이다.

```properties
routing.engine.mode=${ROUTING_ENGINE_MODE:DOCKER}
routing.valhalla.url=${ROUTING_VALHALLA_URL:http://localhost:8000}
routing.motis.url=${ROUTING_MOTIS_URL:http://localhost:8001}
```

## 5. 자동 폴백은 Docker 기능이 아니라 애플리케이션 로직

`RoutingEngineRouter.java`에서 "Valhalla 호출 실패하면 Tmap으로" 하는 건 Docker가 해주는 게 아니라, 애플리케이션 코드가 HTTP 호출 실패/타임아웃을 캐치해서 대체 경로로 넘기는 것이다. Docker는 그저 "컨테이너가 꺼져있으면 그 포트로 접속 자체가 실패한다"는 상황을 만들 뿐이고, 그걸 우아하게 처리하는 건 애플리케이션의 몫이다.

## 6. 자주 쓰는 명령어

```bash
docker ps                   # 지금 떠있는 컨테이너 목록
docker logs -f <컨테이너명>   # Valhalla가 왜 응답 안 하는지 로그 확인
docker stop/start <이름>     # 컨테이너 끄고 켜기
docker exec -it <이름> sh    # 컨테이너 내부 들어가서 디버깅
```

## 요약

| 구분 | Tmap API | Valhalla/MOTIS (Docker) |
|---|---|---|
| 호출 제한 | 있음(과금) | 없음 |
| 실행 위치 | 외부 서버 | 로컬/배포 서버 컨테이너 |
| 장애 시 | 서비스 중단 위험 | 자동으로 Tmap 폴백 |
| 환경 구성 | API 키만 있으면 됨 | OSM 데이터 + 컨테이너 필요 |
