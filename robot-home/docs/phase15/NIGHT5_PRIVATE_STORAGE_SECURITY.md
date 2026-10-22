# Phase15 Night 5 — 私有文件存储安全攻防报告

> Stage 3 | Commit: 4e80aaa | 日期: 2026-10-22 | 验证等级: UNIT_TEST_PASS

## 1. 缺陷发现 (N5-H1)

### 缺陷描述
Night 4实现的私有文件隔离仅包含两层防护：
1. URL前缀校验：`private/` 前缀路由到私有下载端点
2. Bucket校验：`PrivateFileController` 校验bucket名称

**缺失第三层：业务归属授权** — 任何登录用户只要构造 `minio://bucket/private/enterprise/uuid.jpg`，即可下载任意企业的认证材料（营业执照、法人证件等），构成敏感信息泄露。

### 风险等级
HIGH — 企业认证材料属于PII/商业敏感数据，跨企业泄露违反数据安全基线。

## 2. 修复方案

### 安全模型
```
用户身份 → 业务记录反查 → 企业归属 → 授权策略 → 文件对象 → 私有下载
```

### 授权链（PrivateFileAccessService.checkAccess）
| 优先级 | 角色/条件 | 授权结果 |
|--------|-----------|----------|
| 1 | userId == null | 拒绝（请先登录） |
| 2 | ADMIN角色 | 允许（审核认证材料） |
| 3 | 认领申请人（claim.applicantUserId匹配） | 允许 |
| 4 | 认领企业活跃成员（getActiveMember≠null） | 允许 |
| 5 | 资料变更提交人（change.applicantUserId匹配） | 允许 |
| 6 | 变更企业活跃成员 | 允许 |
| 7 | 以上均不匹配 | **fail-closed拒绝** |

### 反查机制
- `extractFileSegment(fileUrl)`：从完整URL提取反查键
  - `minio://bucket/private/enterprise/uuid.jpg` → `private/enterprise/uuid.jpg`
  - `/api/files/private/enterprise/2026/10/22/uuid.jpg` → `enterprise/2026/10/22/uuid.jpg`
  - 其他格式 → `null`（拒绝）
- `CompanyClaimMapper.selectList(LIKE proofMaterials, fileSegment)` — 反查认领记录
- `CompanyProfileChangeMapper.selectList(LIKE newValue, fileSegment)` — 反查资料变更记录

## 3. 攻防测试矩阵 (24/24 PASS)

### 正向授权（5项）
| 测试 | 场景 | 结果 |
|------|------|------|
| FILE-15a | 管理员访问任意私有文件 | PASS |
| 正向-1 | 认领申请人访问自己上传的认证材料 | PASS |
| 正向-2 | 企业活跃成员访问本企业认领材料 | PASS |
| 正向-3 | 资料变更提交人访问变更附件 | PASS |
| 正向-4 | 本地降级路径企业成员授权 | PASS |

### 攻防拒绝（15项）
| 编号 | 攻击场景 | 防御机制 | 结果 |
|------|----------|----------|------|
| FILE-01 | 未登录用户访问 | requireUserId前置 | PASS |
| FILE-02 | 普通用户无业务归属 | LIKE反查无匹配→fail-closed | PASS |
| FILE-03 | 企业A用户访问企业B文件 | companyId归属校验 | PASS |
| FILE-04 | 已退出企业成员 | getActiveMember=null | PASS |
| FILE-05 | 已撤销认证(非申请人) | REJECTED状态+申请人校验 | PASS |
| FILE-06 | 猜测对象地址 | 无匹配业务记录 | PASS |
| FILE-07 | 修改bucket名称 | 反查键不匹配 | PASS |
| FILE-08 | 去掉private前缀 | 反查键不匹配 | PASS |
| FILE-09 | URL编码绕过(%2F) | 编码字符≠原始路径 | PASS |
| FILE-10 | 双重编码绕过(%252F) | 编码字符≠原始路径 | PASS |
| FILE-11 | 路径穿越(../) | 无匹配业务记录 | PASS |
| FILE-12 | 公开静态路径格式 | extractFileSegment返回null | PASS |
| FILE-13 | MinIO endpoint公开URL | extractFileSegment返回null | PASS |
| FILE-14 | 本地降级路径无归属 | 反查无匹配→fail-closed | PASS |
| FILE-15b | 非管理员访问无归属历史文件 | fail-closed | PASS |

### 解析单元测试（4项）
| 测试 | 输入 | 期望输出 | 结果 |
|------|------|----------|------|
| MinIO URL | minio://bucket/private/x.jpg | private/x.jpg | PASS |
| 本地URL | /api/files/private/x.jpg | x.jpg | PASS |
| 不可解析 | https://cdn.example.com/x.jpg | null | PASS |
| 无斜杠MinIO | minio://bucket | null | PASS |

## 4. 修改文件清单
| 文件 | 操作 | 说明 |
|------|------|------|
| `file/PrivateFileAccessService.java` | 新增 | 业务归属授权服务 |
| `file/controller/PrivateFileController.java` | 修改 | 注入checkAccess()调用 |
| `file/PrivateFileAccessServiceTest.java` | 新增 | 24个攻防测试 |

## 5. 验证等级说明
- **UNIT_TEST_PASS**: 24/24单元测试通过（Mock Mapper + ReflectionTestUtils）
- **INTEGRATION_TEST_PASS**: 需要MySQL+MinIO运行时环境（Docker不可用，BLOCKED）
- **RUNTIME_PASS**: 需要完整排练环境（BLOCKED）

## 6. 残留风险
1. LIKE反查性能：proofMaterials为TEXT字段，LIKE '%segment%'全表扫描 — 生产数据量大时需评估索引策略或物化文件归属表
2. 历史数据覆盖：仅反查CompanyClaim和CompanyProfileChange两类业务记录，其他模块引用的私有文件需后续补充反查
3. MinIO Bucket Policy：依赖应用层授权，MinIO服务本身bucket policy应设为私有（部署检查项）