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
COPY --from=builder /build/target/robot-home-server-*.jar app.jar
RUN mkdir -p /app/uploads
ENV TZ=Asia/Shanghai
EXPOSE 8081
HEALTHCHECK --interval=20s --timeout=5s --retries=5 \
  CMD curl -f http://localhost:8081/api/home/index?position=pc || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
