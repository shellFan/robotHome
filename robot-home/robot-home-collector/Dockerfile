# ---- 多阶段构建：Maven 打包 + JRE 运行 ----
FROM maven:3.9-eclipse-temurin-8 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# 创建非 root 用户运行应用
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser
COPY --from=builder /build/target/robot-home-collector-*.jar app.jar
RUN chown -R appuser:appuser /app
ENV TZ=Asia/Shanghai
EXPOSE 8082
USER appuser
HEALTHCHECK --interval=20s --timeout=5s --retries=5 \
  CMD curl -f http://localhost:8082/health || exit 1
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "/app/app.jar"]