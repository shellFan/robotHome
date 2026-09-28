@echo off
setlocal
set "SPRING_DATASOURCE_URL=jdbc:mysql://49.235.70.249:3309/robot_home?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
set "SPRING_DATASOURCE_USERNAME=root"
set "SPRING_DATASOURCE_PASSWORD=Agame#2019_DB"
set "SPRING_REDIS_HOST=localhost"
set "SPRING_REDIS_PORT=6379"
set "SPRING_REDIS_PASSWORD="
set "JWT_SECRET=robot-home-jwt-localdev-2024-Xk9mP2vN8qR5wT3y"
set "FILE_STORAGE_TYPE=local"
set "FILE_MINIO_ACCESS_KEY=minioadmin"
set "FILE_MINIO_SECRET_KEY=minioadmin"
set "ROBOT_SMS_DEV_MODE=true"
set "ADMIN_INIT_USERNAME=admin"
set "ADMIN_INIT_PASSWORD=admin123"
mvn spring-boot:run -q