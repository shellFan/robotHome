# Phase15 Night4 排练环境

隔离排练环境，用于 MySQL 5.6 + Flyway + Nginx + Redis + MinIO 真实运行时验证。

## 快速开始

```bash
cd robot-home

# 1. 前置检查
bash scripts/phase15/preflight.sh

# 2. 启动排练环境
bash scripts/phase15/run-rehearsal.sh

# 3. 运行时验证
bash scripts/phase15/verify-runtime.sh

# 4. 清理
bash scripts/phase15/cleanup-rehearsal.sh
```

## 架构

| 服务 | 容器名 | 端口 | 用途 |
|------|--------|------|------|
| MySQL 5.6 (Fresh) | robot-home-rehearsal-mysql | 13306 | 全新部署路径：initdb初始化→Flyway全量迁移 |
| MySQL 5.6 (Upgrade) | robot-home-rehearsal-mysql-upgrade | 13307 | 升级路径：Phase14历史库→Flyway baseline→增量迁移 |
| Redis 7.2 | robot-home-rehearsal-redis | 16379 | 缓存/韧性验证 |
| MinIO | robot-home-rehearsal-minio | 19000/19001 | 私有文件存储验证 |
| Nginx | robot-home-rehearsal-nginx | 18080 | 反向代理/限流配置验证 |

## 验证项

### MySQL Fresh (13306)
- 19个SQL文件按序执行（01~18，含04_crawler_schema.sql）
- FlywayBaselineGuard 13张必需表全部存在
- 采集系统14张表全部存在
- utf8mb4字符集
- 初始数据行数验证

### MySQL Upgrade (13307)
- Phase14历史库完整性（13张必需表）
- 无flyway_schema_history表（Upgrade路径待baseline）
- 模拟从Phase14升级到Phase15的Flyway迁移

### Redis (16379)
- PING→PONG
- AOF持久化开启

### MinIO (19000)
- 健康检查端点
- 私有文件bucket创建

### Nginx (18080)
- 配置语法检查（nginx -t）
- 三级限流配置加载（auth/payment/api）

## 端口隔离

所有端口使用1xxxx范围，避免与开发环境(3306/6379/8080等)冲突。

## 安全

- 所有凭据为测试凭据（rehearsal_*前缀），不含真实凭据
- restart: "no"，排练环境不自动重启
- 所有资源以robot-home-rehearsal-前缀隔离
- 清理脚本删除所有容器和卷

## 故障排查

```bash
# 查看MySQL初始化日志
docker logs robot-home-rehearsal-mysql

# 查看SQL执行错误
docker logs robot-home-rehearsal-mysql 2>&1 | grep -i error

# 手动进入MySQL
docker exec -it robot-home-rehearsal-mysql mysql -uroot -prehearsal_root_pw robot_home_rehearsal

# 查看表清单
docker exec robot-home-rehearsal-mysql mysql -uroot -prehearsal_root_pw robot_home_rehearsal -e "SHOW TABLES"
```