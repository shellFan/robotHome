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

## 2026-10-30 13:28 本地脚本复核补记

本地复测发现，关键表校验原先对每张表分别使用 `grep -q`。在 `pipefail` 下，`grep` 提前退出可能令 `gunzip` 收到 SIGPIPE，从而把完整的大型备份误判为失败。现改为单次 `awk` 全量扫描，并新增大于 32 MiB 的有效 SQL 压缩样本和缺少 `enterprise_plan` 的负向回归样本。两项当前均通过，日志为 `logs/backup-verifier-tests-current-2026-10-30.log`。

恢复前快照的 Docker 和 standalone 两条 `mysqldump` 命令现显式包含 routines、triggers、events；PowerShell 凭据保护静态检查通过，Bash 语法检查覆盖 5 个脚本并通过。此改动仍是本地代码检查，不代表已连接服务器或创建生产备份；生产备份、校验和隔离恢复仍未执行，生产写入仍为 0。SSH 主机身份未核验，线上操作继续阻塞。

### 13:31 复审修正

独立代码审查发现损坏归档在关键表扫描遇到 `gunzip` 非零退出后会被 `set -e` 中止，未输出末尾汇总。新增损坏 gzip 回归样本先复现了此行为；校验器现将 SQL 扫描失败计为 FAIL 并继续汇总。当前验证器用例覆盖完整大备份、缺必需表、损坏归档三种情况，3/3 PASS；完整日志仍为 `logs/backup-verifier-tests-current-2026-10-30.log`。

### 13:44 当前分支复验

在推送 HEAD `00bcad61c97c724d18f145adc7bdea0520ae40bf` 上再次运行备份校验器 3 个样本和 gzip 故障注入，均 PASS；凭据保护检查 PASS，5 个脚本 `bash -n` PASS。日志：`logs/backup-tests-full-current-2026-10-30.log`、`logs/backup-credential-guard-full-current-2026-10-30.log`。这些均为本地验证，未连接或备份生产数据库。
