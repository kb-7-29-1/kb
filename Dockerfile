# ==========================================
# 1단계: Frontend 빌드 (Vue 3 + Vite)
# ==========================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install

COPY frontend ./
RUN npm run build

# ==========================================
# 2단계: Backend 빌드 (Java 17 + Gradle)
# ==========================================
FROM gradle:8.8-jdk17 AS backend-builder
WORKDIR /app

COPY --chown=gradle:gradle backend ./backend

# 프론트엔드 빌드 결과물을 백엔드 정적 리소스 디렉토리로 이동
COPY --from=frontend-builder /app/frontend/dist ./backend/src/main/webapp/resources

WORKDIR /app/backend
RUN gradle build --no-daemon -x test

# ==========================================
# 3단계: Runtime 실행 (Tomcat 9 + Java 17)
# ==========================================
FROM tomcat:9.0-jdk17-slim
WORKDIR /usr/local/tomcat

# WAR 파일 배치 (ROOT.war로 기본 배포)
COPY --from=backend-builder /app/backend/build/libs/*.war ./webapps/ROOT.war

# Railway 등 클라우드의 동적 포트(PORT) 매핑 지원
EXPOSE 8080
CMD ["sh", "-c", "sed -i 's/port=\"8080\"/port=\"'\"${PORT:-8080}\"'\"/g' conf/server.xml && catalina.sh run"]
