# ---- 多阶段构建：Maven 打包 + JRE 运行 ----
FROM maven:3.9-eclipse-temurin-8 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# 创建非 root 用户运行应用
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser
COPY --from=builder /build/target/robot-home-server-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
ENV TZ=Asia/Shanghai
EXPOSE 8081
USER appuser
HEALTHCHECK --interval=20s --timeout=5s --retries=5 \
  CMD curl -f http://localhost:8081/api/home/index?position=pc || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "/app/app.jar"]
