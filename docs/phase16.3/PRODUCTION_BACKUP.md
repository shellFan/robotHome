# Phase16.3 生产备份与恢复记录

状态：**BLOCKED，未执行生产备份。**

尚未登录服务器：用户暂时无法通过宝塔或云控制台核验服务器 ED25519 指纹 SHA256:niEFrPybZrWYn+fCBpwASJFL7M/hwXpqSGalEpWHRew。未建立可信主机身份前没有发起 SSH 连接。生产数据库地址、数据库版本、表清单、服务账户和磁盘位置仍未知。

Robot Home 本地备份与恢复脚本的安全改动已合并到 main：要求 Docker 备份显式提供密码、限制备份文件权限，并在备份缺失或无效时拒绝恢复。本地 gzip 故障注入回归通过（修复前失败、修复后通过）；恢复测试为 27 PASS、0 FAIL、0 SKIP；凭据保护和 Manifest 测试通过。日志：docs/phase16.3/logs/backup-gzip-failure-post-merge-2026-10-30.log、backup-recovery-post-merge-2026-10-30.log、backup-credential-guard-post-merge-2026-10-30.log、manifest-test-post-merge-2026-10-30.log。

以上是本地脚本验证，不是生产备份，也没有执行隔离库恢复验证。

| 项目 | 结果 |
|---|---|
| 备份时间 | 未执行 |
| 生产范围 | 未确认；预期涉及资讯、分类、产品、品牌、视频、教程、社区、审核及关联表 |
| 备份文件 | 无 |
| 校验 | 未执行 |
| 恢复验证 | 未执行 |
| 生产 DML | 0 |

解除阻塞需要：从可信云控制台或宝塔主机控制台核验 SSH 主机公钥指纹；之后只读确认数据库和受控备份目录，再完成备份、校验和隔离恢复。未取得备份证据前不进行生产数据变更。
