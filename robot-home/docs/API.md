# 机器人之家 Overnight Phase

# 整夜自主开发、真实数据采集、全链路验收与上线收口总指令

项目仓库：

https://github.com/shellFan/robotHome

当前项目已经完成大量基础开发和安全修复。

当前已知状态包括：

```text
Java 8
Spring Boot 2.x
MySQL 5.6
Redis
MinIO
Vue 3 PC
Vue 3 Admin
微信小程序
robot-home-collector
Docker Compose
Nginx
Flyway
```

当前已经完成：

```text
BLOCKER 安全问题清零
HIGH 安全问题清零
CORS 配置化
Swagger 生产环境限制
SVG 上传移除
MinIO Client 复用
logout 空 catch 修复
Crawler Controller 异常处理修复
91 个测试通过
```

不要重复做已经完成的工作。

本轮目标：

# 连续自主工作一个长周期，把“机器人之家”从目前状态推进到真正可以部署、可以采集真实数据、可以持续运营的版本。

---

# 一、最高级执行规则

本轮采用：

# OVERNIGHT AUTONOMOUS MODE

开发过程中：

```text
不要问我是否继续
不要问我下一步做什么
不要问我要不要优化
不要问我是否创建表
不要问我要不要加索引
不要问我用哪个框架
不要问我怎么设计页面
不要问我是否修复某个 Bug
```

除了真正无法自行获得的：

```text
微信 AppID
微信 Secret
第三方付费 API Key
生产服务器 SSH 权限
域名 DNS 权限
必须人工完成的 OAuth
必须人工输入的验证码
```

其他所有事情：

# 自己处理。

---

# 二、工作原则

你的工作循环始终保持：

```text
Inspect
↓
Find Problem
↓
Implement
↓
Compile
↓
Test
↓
Review
↓
Fix
↓
Retest
↓
Commit Checkpoint
↓
Continue
```

不要出现：

```text
分析完成，等待下一步
```

不要停。

---

# 三、不要推翻现有架构

禁止：

```text
重新创建整个 Spring Boot
重新创建整个前端
重新设计所有数据库
重新做一套 Collector
```

应该：

```text
读取现有实现
↓
发现缺口
↓
小步增强
↓
保证兼容
```

---

# 四、Git 安全策略

开始工作前：

```bash
git status
git log --oneline -10
```

记录：

```text
BASE_COMMIT
```

如果存在未提交修改：

先分析。

不要覆盖用户已有修改。

---

## 建议建立工作分支

例如：

```text
phase4-overnight-hardening
```

如果当前环境工作流明确要求直接 main：

可以按现有工作流执行。

但是禁止：

```text
git reset --hard
git clean -fd
force push
```

除非明确确认不会丢失用户代码。

---

# 五、夜间任务总目标

本轮需要尽可能完成以下 12 大领域：

```text
1. 全仓库深度审计
2. MySQL 5.6 最终兼容
3. Collector 真实网站能力
4. 真实机器人行业数据
5. 数据质量
6. PC 真实联调
7. Admin 运营闭环
8. 小程序真实联调
9. 性能优化
10. 稳定性与可观测性
11. Docker / Nginx / 部署
12. 自动化测试与验收
```

---

# 六、TASK 01：全仓库第二次深度审计

不要因为刚修完 BLOCKER/HIGH 就停止审计。

扫描：

```text
robot-home-server
robot-home-collector
robot-home-web
robot-home-admin
robot-home-miniapp
sql
scripts
deploy
docker-compose.yml
docker-compose.prod.yml
```

查找：

```text
TODO
FIXME
return null
UnsupportedOperationException
NotImplemented
mock
fake
hardcode
catch(Exception)
catch(Throwable)
printStackTrace
System.out.println
console.log
localhost
127.0.0.1
admin123
password
secret
token
```

区分：

```text
正常开发配置
真正问题
```

禁止机械删除。

---

# 七、建立夜间审计文件

生成：

```text
docs/OVERNIGHT_AUDIT.md
```

分类：

```text
BLOCKER
HIGH
MEDIUM
LOW
IMPROVEMENT
```

处理规则：

```text
BLOCKER → 必须修
HIGH → 必须修
MEDIUM → 尽量全部修
LOW → 时间允许修
```

最终要求：

```text
BLOCKER = 0
HIGH = 0
```

---

# 八、TASK 02：MySQL 5.6 全项目终审

数据库唯一标准：

# MySQL 5.6

扫描：

```text
.sql
Mapper XML
@Select
@Update
@Insert
QueryWrapper
原生 SQL
Flyway migration
测试 SQL
Docker
```

禁止：

```text
WITH
CTE
ROW_NUMBER
RANK() OVER
DENSE_RANK
窗口函数
JSON 类型
JSON_EXTRACT
JSON_SET
JSON_OBJECT
utf8mb4_0900_ai_ci
MySQL 8 专用语法
```

---

# 九、MySQL 5.6 索引限制

重点检查：

```text
utf8mb4 + VARCHAR 长索引
```

长 URL 不直接全文建普通索引。

推荐：

```text
url_hash CHAR(64)
content_hash CHAR(64)
```

对 Hash 建索引。

---

# 十、Flyway 重点验证

当前已经加入 Flyway。

必须测试：

### 场景 A

```text
空数据库
↓
第一次启动
↓
migration 全部成功
```

### 场景 B

```text
已有数据库
↓
升级
↓
migration 成功
```

### 场景 C

```text
重复启动
↓
不会重复执行 migration
```

### 场景 D

```text
migration 中途失败
↓
日志清晰
↓
可以定位
```

---

# 十一、数据库数据不能破坏

禁止为了测试：

```text
DROP 现有正式库
TRUNCATE 正式业务表
DELETE 全表
```

开发验证优先：

```text
独立测试数据库
Docker MySQL
临时 schema
```

---

# 十二、TASK 03：Collector 深度增强

这是本轮最重要任务。

不要再只证明：

```text
Collector 能启动
```

必须证明：

# Collector 真能持续产生机器人之家需要的数据。

---

# 十三、Collector 最终链路

要求：

```text
crawler_source
↓
robots.txt
↓
sitemap
↓
rss
↓
URL Discovery
↓
Queue
↓
Fetch
↓
Dynamic Render fallback
↓
Page Classification
↓
Article Extract
↓
Product Extract
↓
Content Clean
↓
Image Localization
↓
Deduplicate
↓
Brand Matching
↓
Company Matching
↓
Robot Matching
↓
Quality Validation
↓
Review
↓
Publish
↓
Robot Home DB
```

逐项检查。

缺哪个补哪个。

---

# 十四、真实网站测试

至少测试：

```text
15 个真实机器人相关官方网站
```

尽量包括：

```text
宇树科技
优必选
智元机器人
傅利叶智能
普渡机器人
擎朗智能
越疆机器人
节卡机器人
遨博
达闼
Boston Dynamics
Figure
Agility Robotics
其他公开机器人品牌
```

最终选择根据：

```text
robots.txt
官网开放程度
页面结构
```

自行调整。

---

# 十五、抓取原则

必须：

```text
尊重 robots.txt
合理 Crawl Delay
低并发
不绕验证码
不绕登录
不破解接口
不绕付费墙
```

遇到明确阻止：

```text
BLOCKED
```

继续其他来源。

不要整个任务停掉。

---

# 十六、页面分类

Collector 需要区分：

```text
HOME
LIST
ARTICLE
PRODUCT
COMPANY
BRAND
CATEGORY
OTHER
```

不要所有页面都走 Article Parser。

---

# 十七、文章抽取质量

必须重点优化正文抽取。

要求文章正文尽量不包含：

```text
导航
Header
Footer
Cookie
登录
注册
联系方式大块
隐私政策
友情链接
相关推荐
广告
```

保留：

```text
H1/H2/H3
正文
图片
列表
表格
引用
```

---

# 十八、产品页抽取

机器人产品至少尝试抽取：

```text
product_name
model
brand
company
category
summary
description
cover
gallery
official_url
release_date
price
specifications
```

抓不到：

允许 NULL。

禁止猜。

---

# 十九、参数标准化

继续强化：

```text
raw_name
raw_value
normalized_name
normalized_value
unit
```

例如：

```text
整机重量
本体重量
净重
Weight
Net Weight
```

统一：

```text
weight
```

---

# 二十、重点参数词典

至少覆盖：

```text
height
weight
width
length
dof
joint_count
max_speed
payload
battery
battery_capacity
runtime
charging_time
cpu
gpu
npu
compute_power
camera
depth_camera
lidar
imu
microphone
wifi
bluetooth
4g
5g
sdk
api
ros
ros2
python
cpp
```

---

# 二十一、单位标准化

例如：

```text
1800 mm
180 cm
1.8 m
```

可以标准化。

同时：

# 永远保留 raw_value。

---

# 二十二、数据变化追踪

产品官网发生变化：

```text
旧参数
↓
新参数
```

必须记录：

```text
robot_id
parameter_key
old_value
new_value
source_url
detect_time
```

不要直接无痕覆盖。

---

# 二十三、Collector URL 去重

必须完善：

```text
Canonical URL
url_hash
```

处理：

```text
utm_source
utm_medium
utm_campaign
spm
from
fragment
```

但：

不要误删必要业务参数。

---

# 二十四、内容去重

组合：

```text
source_url
url_hash
content_hash
title similarity
SimHash
```

避免：

```text
官网同一文章多个语言 URL
手机 URL
带 tracking URL
```

重复入库。

---

# 二十五、重复数据处理

状态明确：

```text
NEW
UPDATED
DUPLICATE
SKIPPED
```

后台可以看到重复原因。

---

# 二十六、动态网页 fallback

优先：

```text
HTTP Fetcher
```

发现：

```text
正文过短
关键节点不存在
需要 JS 渲染
```

自动 fallback：

```text
BrowserFetcher
```

如果当前是 Selenium：

继续用成熟实现。

如果当前已经是 Playwright：

保持。

不要无必要更换技术栈。

---

# 二十七、Browser 资源治理

避免：

```text
每抓一个页面启动一个 Chrome
```

浏览器 / Context 应合理复用。

限制：

```text
最大并发
页面超时
内存
Context 生命周期
```

防止运行几个小时后内存爆炸。

---

# 二十八、Collector 内存泄漏检查

重点检查：

```text
browser page 未关闭
stream 未关闭
response 未关闭
线程池
scheduled executor
大量 byte[]
HTML Document 长期缓存
```

夜间长跑 Collector 必须稳定。

---

# 二十九、任务超时恢复

已经有 recoverTimedOut。

继续验证：

```text
任务执行中 Collector 重启
↓
旧任务不会永久 RUNNING
↓
超时任务可以恢复 / 标记失败
```

---

# 三十、重试策略

当前已有指数退避。

继续确保：

```text
网络超时 → retry
5xx → retry
429 → delayed retry
404 → 通常不 retry
401/403 → 不无限 retry
BLOCKED → 不 retry
```

---

# 三十一、429

支持：

```text
Retry-After
```

如果存在。

---

# 三十二、域名级限流

不要只有全局限流。

建议：

```text
domain -> limiter
```

不同站点独立限速。

---

# 三十三、TASK 04：真实行业数据

当前 SQL 已经有真实品牌企业和机器人数据。

继续完善，而不是重复造 Demo。

目标：

```text
真实品牌 >= 50
真实企业 >= 50
真实机器人产品 >= 100
```

如果已有数量接近：

补充不足。

---

# 三十四、真实数据规则

正式数据必须：

```text
有来源
可核验
```

不要生成：

```text
假价格
假参数
假公司介绍
假融资
假发布时间
```

无法确定：

```text
NULL
```

---

# 三十五、data_source

所有业务数据建议清晰区分：

```text
MANUAL
OFFICIAL
CRAWLER
DEMO
IMPORT
```

生产环境默认不优先展示 DEMO。

---

# 三十六、TASK 05：文章内容库存

通过真实公开来源，尽量累积：

```text
机器人行业有效文章 >= 300
```

优先：

```text
官方新品
企业新闻
技术文章
产品新闻
发布会
行业动态
```

---

# 三十七、文章关联

自动关联：

```text
brand
company
robot
```

建立置信度：

```text
100
90
80...
```

例如：

```text
标题精确包含型号
+ 官方来源
```

高置信度。

---

# 三十八、低置信度

不要硬关联。

状态：

```text
MATCH_PENDING
```

---

# 三十九、TASK 06：图片生产链路

真实内容需要：

```text
图片下载
↓
SHA256
↓
去重
↓
图片校验
↓
压缩
↓
上传 MinIO
```

---

# 四十、图片类型

只允许安全格式：

```text
jpg
jpeg
png
webp
gif
```

SVG 已经禁止：

不要重新加回来。

---

# 四十一、缩略图

至少：

```text
thumbnail
medium
original
```

列表：

thumbnail。

详情：

medium / original。

---

# 四十二、图片 EXIF

如果实现成本合理：

可以去除不必要 EXIF。

特别是：

用户上传图片。

---

# 四十三、TASK 07：Admin 采集运营中心

后台必须可以真正管理 Collector。

至少包含：

```text
数据源
任务
URL
文章结果
产品结果
匹配记录
错误记录
重复数据
参数变化
```

---

# 四十四、数据源页面

运营人员可以：

```text
新增
编辑
启用
停用
测试连接
立即抓取
查看最近一次结果
```

---

# 四十五、采集预览

文章：

```text
原标题
解析标题
发布时间
来源
封面
正文
匹配品牌
匹配企业
匹配机器人
状态
```

产品：

```text
名称
型号
图片
参数
来源
匹配品牌
匹配企业
```

---

# 四十六、人工修正

运营人员发布前可以修改：

```text
标题
摘要
分类
封面
品牌
企业
机器人关联
```

不要直接修改 crawler raw data。

应该修改：

```text
parsed / staging record
```

保留原始数据。

---

# 四十七、TASK 08：PC 端全面体验验收

不要只检查 build。

必须跑真实 API。

页面：

```text
首页
机器人库
机器人详情
机器人参数
机器人图片
机器人视频
机器人对比
排行榜
品牌
品牌详情
企业
企业详情
资讯
资讯详情
视频
社区
教程
搜索
登录
用户中心
收藏
历史
询价
```

---

# 四十八、PC 空状态

所有列表必须正确处理：

```text
loading
empty
error
retry
```

禁止：

```text
接口失败后白屏
```

---

# 四十九、详情 404

不存在：

```text
robot/99999999
```

应该显示：

```text
内容不存在
```

而不是 Vue 报错。

---

# 五十、SEO

PC 重点完善：

```text
title
description
keywords
canonical
robots.txt
sitemap.xml
OpenGraph
```

至少对：

```text
机器人详情
品牌详情
企业详情
文章详情
```

生成合理 Metadata。

---

# 五十一、动态 SEO

如果现在 Vue SPA 无法真正服务器动态输出：

先建立：

```text
SEO metadata service
```

以及：

```text
sitemap
```

未来预留 SSR/Nuxt。

不要本轮大规模重构成 Nuxt。

---

# 五十二、TASK 09：小程序全量回归

检查：

```text
首页
搜索
机器人
详情
参数
品牌
企业
资讯
社区
教程
收藏
浏览历史
我的
```

---

# 五十三、小程序重点问题

检查：

```text
setData 大对象
长列表
图片尺寸
API timeout
Token expiry
401
空状态
弱网
重复点击
```

---

# 五十四、API 基址

禁止把：

```text
localhost
测试 IP
```

写死在生产小程序代码。

区分：

```text
dev
test
prod
```

---

# 五十五、TASK 10：搜索优化

真实验证：

```text
宇树
Unitree
G1
Go2
机器狗
人形机器人
ROS2
协作机器人
配送机器人
```

搜索：

```text
机器人
品牌
企业
文章
教程
社区
```

---

# 五十六、MySQL 5.6 搜索性能

第一阶段不用 Elasticsearch。

合理使用：

```text
索引
LIKE 前缀
搜索冗余字段
Redis 热搜
```

禁止出现：

几十万数据后明显不可用的查询。

---

# 五十七、TASK 11：排行榜

排行榜不允许静态假数据。

检查热度计算。

至少考虑：

```text
views
favorites
comments
compares
inquiries
```

加入时间衰减。

---

# 五十八、Redis ZSET

热榜可以使用：

```text
ZSET
```

数据库：

保存必要快照。

---

# 五十九、TASK 12：机器人对比

完整验证：

```text
2 台
3 台
4 台
```

支持：

```text
差异高亮
隐藏相同
参数分组
缺失值
```

---

# 六十、TASK 13：接口一致性

所有接口统一：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

检查有没有例外。

---

# 六十一、HTTP 状态码

不要全部永远：

```text
HTTP 200
code = 500
```

安全和认证场景合理使用：

```text
400
401
403
404
429
500
```

根据现有项目约定保持统一。

---

# 六十二、TASK 14：N+1

重点检查列表：

```text
机器人列表
文章列表
评论
收藏
后台任务
```

避免：

```text
查20条列表
再循环查20次品牌
再循环查20次图片
```

必要时：

```text
批量查询
JOIN
Map组装
```

---

# 六十三、数据库慢 SQL

使用：

```text
EXPLAIN
```

检查：

```text
Robot List
Search
Article List
Crawler URL
Crawler Task
Comments
Favorites
```

生成：

```text
docs/SQL_OPTIMIZATION.md
```

---

# 六十四、TASK 15：缓存

检查：

```text
机器人详情
品牌
排行榜
热搜
```

合理缓存。

但避免：

```text
更新数据库后缓存长期不刷新
```

必须有失效机制。

---

# 六十五、缓存击穿保护

热点页面必要时：

```text
短 TTL
随机 TTL
mutex
```

不用过度复杂。

---

# 六十六、TASK 16：Redis 故障降级

测试：

```text
Redis 关闭
```

普通：

```text
机器人浏览
文章浏览
```

是否还能基本工作。

登录等强依赖功能：

应给清晰错误。

不能 NPE。

---

# 六十七、TASK 17：MinIO 故障

测试：

```text
MinIO 不可达
```

文件上传：

返回明确错误。

普通列表：

不要整个服务启动失败。

除非生产配置明确要求 MinIO 为必须依赖。

---

# 六十八、TASK 18：日志

生产日志：

```text
server
collector
```

分别可定位。

配置：

```text
rolling
max file size
retention
```

避免磁盘打满。

---

# 六十九、请求 Trace

如果当前成本不高：

增加：

```text
requestId / traceId
```

日志中贯穿：

```text
Controller
Service
Error
```

方便生产排查。

---

# 七十、Collector Trace

至少：

```text
taskId
sourceId
urlId
```

日志必须带上。

---

# 七十一、TASK 19：Actuator / Health

Server：

```text
/actuator/health
```

Collector：

```text
/actuator/health
```

生产环境不要暴露所有敏感 actuator endpoint。

---

# 七十二、Health Detail

至少内部判断：

```text
DB
Redis
Disk
```

Collector：

```text
DB
Browser availability
```

---

# 七十三、TASK 20：Docker

真实执行：

```bash
docker compose build
docker compose up -d
```

检查：

```text
mysql
redis
minio
backend
collector
nginx
```

---

# 七十四、MySQL Docker

必须：

```text
mysql:5.6
```

不是 8。

---

# 七十五、Docker Healthcheck

关键服务增加：

```text
healthcheck
```

避免：

```text
MySQL 未启动
backend 已经疯狂重启
```

---

# 七十六、TASK 21：Nginx

验证：

```text
/
 /admin/
 /api/
 /files/
```

刷新：

```text
/admin/users
/robot/1
/article/1
```

不能 404。

---

# 七十七、Compression

Nginx 开启合理：

```text
gzip
```

静态资源缓存：

```text
js
css
image
```

---

# 七十八、安全 Header

合理添加：

```text
X-Content-Type-Options
X-Frame-Options
Referrer-Policy
```

CSP 如果可能破坏现有业务：

谨慎配置。

不要为了安全头导致站点不可用。

---

# 七十九、TASK 22：生产配置审查

重点：

```text
prod profile
```

生产禁止：

```text
Swagger
sms dev mode
crawler dev mode
默认弱密码
详细异常堆栈暴露
```

---

# 八十、配置优先环境变量

所有敏感配置：

```text
DB_PASSWORD
REDIS_PASSWORD
MINIO_SECRET
JWT_SECRET
CRAWLER_API_KEY
```

环境变量。

---

# 八十一、TASK 23：自动化测试扩充

当前已有：

```text
91 tests PASS
```

不要为了追数量写无意义测试。

新增重点：

```text
Collector 真正关键路径
MySQL 5.6 特殊 SQL
权限
发布幂等
重复数据
异常恢复
```

---

# 八十二、关键 Collector Tests

至少覆盖：

```text
URL Normalize
robots
Sitemap
RSS
HTML extract
Product extract
Date parse
Hash
SimHash
Duplicate
Retry
Timeout recovery
Publish validation
Transaction rollback
```

---

# 八十三、TASK 24：Integration Test

完整：

```text
Source
↓
Task
↓
Fetch
↓
Parse
↓
Persist staging
↓
Review
↓
Publish
```

至少一条自动化集成测试。

---

# 八十四、TASK 25：Smoke Test

建立：

```text
scripts/smoke-test.*
```

或者：

```text
tests/smoke
```

至少测试：

```text
health
login
robots
robot detail
brands
articles
search
admin login
collector health
```

---

# 八十五、TASK 26：前端 build

Web：

```bash
npm ci
npm run build
```

Admin：

```bash
npm ci
npm run build
```

处理所有：

```text
TypeScript Error
Warning中真正有风险的问题
```

---

# 八十六、Bundle 检查

如果单个 JS bundle 特别巨大：

合理：

```text
lazy route
dynamic import
```

不要过度优化。

---

# 八十七、TASK 27：Server 构建

执行：

```bash
mvn clean test
mvn clean package
```

必须 PASS。

---

# 八十八、Collector 构建

执行：

```bash
mvn clean test
mvn clean package
```

必须 PASS。

---

# 八十九、TASK 28：长时间 Collector 稳定性

如果环境允许：

让 Collector 连续跑多批任务。

观察：

```text
Memory
Threads
DB Connections
Browser processes
Failed tasks
Queue
```

修复明显泄漏。

---

# 九十、TASK 29：数据库连接池

检查：

```text
Hikari
```

不要：

```text
连接泄漏
连接数无限增长
```

设置合理：

```text
maxPoolSize
timeout
idle
lifetime
```

---

# 九十一、TASK 30：请求超时

外部 HTTP：

必须：

```text
connect timeout
read timeout
```

不能无限等待。

---

# 九十二、TASK 31：前端异常体验

处理：

```text
401 → 登录
403 → 无权限
404 → 页面不存在
500 → 友好错误
network error → 重试提示
```

---

# 九十三、TASK 32：数据备份文档

生成：

```text
docs/BACKUP_RESTORE.md
```

包含：

```text
MySQL backup
MySQL restore
MinIO backup
Redis 是否需要备份
```

---

# 九十四、TASK 33：上线清单

生成：

```text
docs/PRODUCTION_CHECKLIST.md
```

包含：

```text
域名
HTTPS
MySQL
Redis
MinIO
环境变量
Nginx
后端
Collector
Web
Admin
MiniApp
Backup
Monitoring
Logs
Security
```

---

# 九十五、TASK 34：恢复文档

生产出现问题时：

```text
Server 无法启动
DB migration失败
Redis异常
Collector异常
MinIO异常
Nginx异常
```

写基本排查方法。

---

# 九十六、TASK 35：微信公众号

不要把一晚时间浪费在绕过微信限制。

本轮只确保：

```text
WeChatSourceAdapter
公开文章URL导入
批量URL导入
文章解析
图片本地化
来源记录
去重
入库
```

架构正确。

需要官方授权：

```text
REQUIRES_AUTH
```

即可。

---

# 九十七、禁止行为

禁止：

```text
为了测试通过删功能
```

禁止：

```text
把 failing test 直接 @Disabled
```

除非测试本身已经无效，并清楚说明原因。

---

# 九十八、禁止假成功

不能：

```text
没跑 Docker
却写 Docker PASS
```

不能：

```text
没访问真实网站
却写 Crawler PASS
```

不能：

```text
没跑 MySQL 5.6
却写 MYSQL56 PASS
```

最终报告只能写真正执行过的。

---

# 九十九、无法执行的外部条件

如果确实因为：

```text
Docker 不可用
Chrome 不可用
网络不可达
无微信 AppID
```

记录：

```text
ENVIRONMENT_BLOCKED
```

然后继续其他所有可以完成的工作。

不能一个环境问题让整个夜间工作停止。

---

# 一百、代码质量

保持：

```text
Controller 薄
Service 业务
Mapper 数据
DTO / VO 分离
```

避免：

```text
God Service
God Controller
几千行类
复制粘贴
```

---

# 一百零一、事务

写操作：

```text
产品发布
文章发布
关联表更新
```

继续确保：

```text
@Transactional
```

边界正确。

外部长时间 HTTP 调用：

不要放数据库长事务里。

---

# 一百零二、并发

重点检查：

```text
重复收藏
重复点赞
重复发布
重复抓取
库存式计数
```

依靠：

```text
唯一索引
幂等
事务
锁
```

而不是只靠前端按钮禁用。

---

# 一百零三、计数一致性

例如：

```text
view_count
favorite_count
comment_count
```

不要出现：

```text
负数
无限重复增加
```

---

# 一百零四、删除策略

采集内容：

不要轻易物理删除。

业务支持：

```text
下架
删除标记
source_missing
```

---

# 一百零五、源码来源

正式文章和产品：

保留：

```text
source_name
source_url
crawl_time
last_verified_time
```

---

# 一百零六、后台操作日志

检查：

```text
发布
下架
删除
修改
审核
```

重要后台行为有日志。

---

# 一百零七、夜间 Commit 策略

每完成一个较大阶段：

可以 commit。

例如：

```text
fix: collector real-world crawling stability

feat: improve crawler product extraction

perf: optimize robot search and indexes

feat: complete crawler admin review workflow

test: add production smoke tests

docs: add production checklist
```

不要：

```text
每改一行提交一次
```

---

# 一百零八、最终统一 Review

全部完成后：

再次执行：

```bash
git diff BASE_COMMIT..HEAD
```

检查：

```text
误删
调试代码
临时配置
真实密码
console debug
硬编码测试地址
```

---

# 一百零九、最终测试矩阵

必须尽可能执行：

```text
SERVER TEST
COLLECTOR TEST
SERVER PACKAGE
COLLECTOR PACKAGE
WEB BUILD
ADMIN BUILD
MYSQL 5.6 INIT
FLYWAY
REDIS
MINIO
DOCKER
NGINX
SMOKE
CRAWLER REAL WORLD
```

---

# 一百一十、完成标准

只有：

```text
BLOCKER = 0
HIGH = 0
```

才能宣布：

```text
OVERNIGHT PHASE COMPLETE
```

MEDIUM：

尽量清零。

如果有剩余：

说明原因。

---

# 一百一十一、最终报告

最终只输出：

# ROBOT HOME OVERNIGHT PHASE COMPLETE

报告格式：

```text
BASE COMMIT:
HEAD COMMIT:
TOTAL COMMITS:

====================
QUALITY
====================

BLOCKER:
HIGH:
MEDIUM:
LOW:

====================
BUILD
====================

Server Tests:
Server Package:
Collector Tests:
Collector Package:
Web Build:
Admin Build:

====================
DATABASE
====================

MySQL 5.6:
Flyway Fresh DB:
Flyway Existing DB:
Indexes Reviewed:

====================
INFRA
====================

Redis:
MinIO:
Docker:
Nginx:
Health Checks:

====================
CRAWLER
====================

Websites Tested:
Websites Success:
Websites Blocked:
Pages Crawled:
Articles Parsed:
Articles Published:
Products Parsed:
Products Published:
Duplicates Filtered:
Images Localized:
Failed URLs:

====================
APPLICATION
====================

PC Smoke:
Admin Smoke:
MiniApp API Smoke:
Search:
Ranking:
Compare:
Inquiry:

====================
SECURITY
====================

Secrets Scan:
Admin Permission:
Crawler API Auth:
Upload Security:
CORS:
Swagger Prod:

====================
DOCUMENTATION
====================

OVERNIGHT_AUDIT:
CRAWLER_REAL_WORLD_REPORT:
SQL_OPTIMIZATION:
PRODUCTION_CHECKLIST:
BACKUP_RESTORE:

====================
GIT
====================

Git Status:
Git Push:

====================
REMAINING
====================

Environment Blockers:
Medium Issues:
Low Issues:
Next Recommended Phase:
```

---

# 一百一十二、Git Push

确认最终代码没有：

```text
真实密码
target
node_modules
日志
临时 dump
IDE 临时文件
```

然后：

```bash
git add
git commit
git push
```

如果已经分阶段 commit：

最终确保全部 push 到 GitHub。

---

# 一百一十三、今晚重点优先级

如果时间和计算资源有限：

严格按照：

```text
P0
真实 Collector
MySQL 5.6
全链路数据发布
测试
稳定性

↓

P1
PC/Admin/小程序联调
性能
Docker
Nginx

↓

P2
SEO
文档
细节优化
```

不要把大量时间浪费在低价值 UI 微调。

---

# 一百一十四、最核心的目标

今晚结束以后：

系统至少应该做到：

```text
添加机器人官网
↓
真正抓到网页
↓
真正解析文章
↓
真正解析产品
↓
真正下载图片
↓
真正去重
↓
真正匹配品牌/企业/机器人
↓
后台真正审核
↓
真正写入 MySQL 5.6
↓
PC 真正显示
↓
小程序 API 真正显示
```

如果这条链没有打通：

不要把时间用于新增商城、AI 推荐、招聘等新模块。

---

# 一百一十五、现在开始执行

现在：

```text
读取最新 GitHub / 本地代码
↓
记录 BASE_COMMIT
↓
全仓库扫描
↓
建立 Overnight Audit
↓
按 P0/P1/P2 顺序执行
↓
连续开发
↓
持续测试
↓
持续修复
↓
阶段提交
↓
最终全量回归
↓
push GitHub
↓
输出最终结果
```

不要中途等待我说：

```text
继续
```

# 从现在开始直接进入整夜自主开发模式。
# 机器人之家 API 契约（第一版）

- 后端基址：`http://localhost:8081`，所有接口前缀 `/api`
- 统一返回结构：

```json
{ "code": 200, "message": "success", "data": {}, "timestamp": 1710000000000 }
```

- 分页结构：

```json
{ "pageNum": 1, "pageSize": 20, "total": 100, "pages": 5, "list": [] }
```

- 认证：请求头 `Authorization: Bearer <token>`
  - 前台用户 token：`POST /api/auth/login`、`/api/auth/register`、`/api/auth/sms-login`、`/api/auth/wx-login`
  - 后台管理员 token：`POST /api/admin/auth/login`（账号密码由环境变量 ADMIN_INIT_USERNAME / ADMIN_INIT_PASSWORD 配置）
- 错误码：200 成功、400 参数校验、401 未登录、403 无权限、404 未找到、500 业务/系统异常

---

## 一、首页

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/home/index?position=pc\|app` | 首页聚合：banners / quickNav / hotRobots / newRobots / hotBrands / articles / videos / hotPosts / companies / rankings |

`rankings` 结构：`{ code, name, robots: RobotListVO[] }`

---

## 二、机器人

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/robots` | 分页筛选。参数：categoryId, brandId, seriesId, keyword, minPrice, maxPrice, scenes(逗号分隔), devs, ais, releaseStart, releaseEnd, sort(comprehensive\|hot\|price_asc\|price_desc\|new\|score), pageNum, pageSize |
| GET | `/api/robots/filters` | 筛选项：categories(树) / brands / scenes / devs / ais / priceRanges |
| GET | `/api/robots/hot?limit=10` | 热门机器人 |
| GET | `/api/robots/new?limit=10` | 新品机器人 |
| GET | `/api/robots/compare?ids=1,2,3` | 参数对比（最多 4 台）。返回 `{ robots:[{id,name,model,coverImage,brandName,guidePrice}], groups:[{groupName, rows:[{defId,paramName,unit,values:[],different}]}] }` |
| GET | `/api/robots/{id}` | 详情：robot / brandName / brandLogo / categoryName / companyName / images / videos / paramGroups / prices / articles / scenes / devs / ais / favorited |
| GET | `/api/robots/{id}/params` | 参数分组：`[{ group:{id,name,sort}, defs:[{ def:{id,name,unit,type,options,isCompare,isShow}, value }] }]` |
| GET | `/api/robots/{id}/images` | 图片 |
| GET | `/api/robots/{id}/videos` | 视频 |
| GET | `/api/robots/{id}/articles?limit=10` | 关联资讯 |
| POST | `/api/robots/{id}/view` | 上报浏览（可选） |

RobotListVO 字段：id, name, model, subtitle, brandId, brandName, brandLogo, categoryId, categoryName, coverImage, guidePrice, marketPrice, mainParams, status, releaseDate, hotScore, viewCount, favoriteCount, compareCount, commentCount, inquiryCount, score, favorited, tags[]

---

## 三、品牌 / 企业

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/brands?keyword=&initial=&hot=&pageNum=&pageSize=` | 品牌分页 |
| GET | `/api/brands/letters` | 首字母分组 `[{letter, brands[]}]` |
| GET | `/api/brands/hot?limit=12` | 热门品牌 |
| GET | `/api/brands/{id}` | 品牌详情：brand / companyName / productCount / products[] |
| GET | `/api/brands/{id}/robots` | 品牌下的机器人（分页） |
| GET | `/api/companies?keyword=&region=&pageNum=&pageSize=` | 企业分页 |
| GET | `/api/companies/regions` | 地区列表 |
| GET | `/api/companies/hot?limit=10` | 热门企业 |
| GET | `/api/companies/{id}` | 企业详情：company / brandList / productList |
| GET | `/api/companies/{id}/robots` | 企业下的机器人（分页） |

---

## 四、内容：资讯 / 视频 / 教程

| 方法 | 路径 |
|---|---|
| GET | `/api/articles?categoryId=&keyword=&pageNum=&pageSize=` |
| GET | `/api/articles/categories` （`[{id,name,sort,count}]`）|
| GET | `/api/articles/hot?limit=10` |
| GET | `/api/articles/{id}` （含 content / tags / prev / next / liked / favorited）|
| GET | `/api/videos?categoryId=&keyword=&pageNum=&pageSize=` |
| GET | `/api/videos/categories` |
| GET | `/api/videos/hot?limit=10` |
| GET | `/api/videos/{id}` （含 url / related）|
| GET | `/api/tutorials?categoryId=&keyword=&pageNum=&pageSize=` |
| GET | `/api/tutorials/categories` |
| GET | `/api/tutorials/hot?limit=10` |
| GET | `/api/tutorials/{id}` （Markdown 正文）|

---

## 五、社区

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/community/circles` | 圈子列表 |
| GET | `/api/community/posts?circleId=&topic=&keyword=&sort=latest\|hot&pageNum=&pageSize=` | 帖子分页 |
| GET | `/api/community/topics?limit=10` | 热门话题 |
| GET | `/api/community/posts/{id}` | 帖子详情 |
| POST | `/api/community/posts` | 发帖（需登录）。body: `{circleId,title,content,images[],videoUrl,robotId,brandId,topic}` |
| DELETE | `/api/community/posts/{id}` | 删帖（本人） |
| GET | `/api/community/my-posts` | 我的帖子 |

---

## 六、互动（收藏 / 点赞 / 关注 / 评论 / 历史）

bizType 取值：`robot` / `article` / `video` / `tutorial` / `post` / `comment`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/favorites?bizType=&pageNum=&pageSize=` | 我的收藏（回填 title/image/url） |
| POST | `/api/favorites?bizType=&bizId=` | 切换收藏，返回 `{favorited:true}` |
| DELETE | `/api/favorites/{bizType}/{bizId}` | 取消收藏 |
| GET | `/api/favorites/check?bizType=&bizId=` | 是否已收藏 |
| GET | `/api/favorites/count` | 收藏数 |
| POST | `/api/likes?bizType=&bizId=` | 切换点赞，返回 `{liked:true}` |
| GET | `/api/likes/check?bizType=&bizId=` | 是否已点赞 |
| GET | `/api/likes/count` | 我点过的赞数 |
| GET | `/api/follows?followType=&pageNum=&pageSize=` | 我的关注 |
| POST | `/api/follows?followType=&followId=` | 切换关注，返回 `{followed:true}` |
| DELETE | `/api/follows/{followType}/{followId}` | 取消关注 |
| GET | `/api/follows/check?followType=&followId=` | 是否已关注 |

followType 取值：`user` / `brand` / `company` / `robot`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/comments?bizType=&bizId=&pageNum=&pageSize=` | 一级评论（每条含 replies 预览 3 条） |
| GET | `/api/comments/{id}/replies` | 某评论的全部回复 |
| POST | `/api/comments` | 发表评论。body: `{bizType,bizId,content,parentId,replyTo}` |
| DELETE | `/api/comments/{id}` | 删除本人评论 |
| POST | `/api/comments/{id}/like` | 评论点赞 |
| GET | `/api/comments/my` | 我的评论 |
| GET | `/api/comments/count?bizType=&bizId=` | 评论数 |
| GET | `/api/history?bizType=&pageNum=&pageSize=` | 浏览历史 |
| DELETE | `/api/history/{bizType}/{bizId}` | 删除单条 |
| DELETE | `/api/history?bizType=` | 清空 |

---

## 七、搜索

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/search?keyword=&limit=5` | 综合搜索，按类型分组返回 robots/brands/companies/articles/videos/tutorials/posts + counts |
| GET | `/api/search/{type}?keyword=&pageNum=&pageSize=` | 指定类型分页搜索（type=robot\|brand\|company\|article\|video\|tutorial\|post） |
| GET | `/api/search/suggest?keyword=&limit=10` | 搜索联想 |
| GET | `/api/search/hot?limit=10` | 热搜 |
| GET | `/api/search/history?limit=10` | 搜索历史（登录） |
| DELETE | `/api/search/history` | 清空搜索历史 |

---

## 八、排行榜 / 询价 / 消息 / 用户

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/rankings?type=hot&limit=20` | 榜单数据（热度权重算法） |
| GET | `/api/rankings/types` | 榜单类型 7 种：hot/humanoid/quadruped/service/industrial/family/dev |
| POST | `/api/inquiries` | 提交询价（可不登录）。body: `{robotId,name,phone,region,customerType,companyName,quantity,budget,remark}` |
| GET | `/api/inquiries/my` | 我的询价 |
| GET | `/api/inquiries/my/{id}` | 我的询价详情 |
| GET | `/api/messages` | 消息列表 |
| GET | `/api/messages/unread-count` | 未读数 |
| POST | `/api/messages/{id}/read` | 标记已读 |
| POST | `/api/messages/read-all` | 全部已读 |
| GET | `/api/users/me` | 我的资料 |
| PUT | `/api/users/me` | 更新资料 `{nickname,avatar,intro,gender,province,city,email}` |
| GET | `/api/users/me/stats` | 用户中心统计 |
| GET | `/api/users/{id}` | 他人主页 |
| GET | `/api/users/me/posts` / `me/comments` / `me/favorites` / `me/history` / `me/follows` / `me/likes` | 用户中心各列表 |

---

## 九、Banner / 推荐位 / 文件

| 方法 | 路径 |
|---|---|
| GET | `/api/banners?position=pc\|app` |
| GET | `/api/recommends/{code}` （code: hot_robot / new_robot / recommend_article / recommend_video / recommend_company）|
| POST | `/api/files/upload` （multipart，字段 `file`，模块 `module`；需登录，返回 `{url,name,size}`）|

---

## 十、管理后台（需 admin token，前缀 `/api/admin`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/auth/login?username=&password=` | 返回 `{token, expiresIn, username, nickname}` |
| GET | `/api/admin/auth/info` | 管理员信息 + roles + permissions + menus(树) |
| POST | `/api/admin/auth/logout` | 登出 |
| GET | `/api/admin/dashboard/stats` | 统计：userCount/todayNewUserCount/robotCount/brandCount/companyCount/articleCount/videoCount/tutorialCount/postCount/inquiryCount/pendingInquiryCount/commentCount/todayPv/todayUv |
| GET | `/api/admin/dashboard/trend?days=7` | 趋势 `[{date,pv,uv,newUsers,inquiries}]` |

### 机器人管理

| 方法 | 路径 |
|---|---|
| GET | `/api/admin/robots?keyword=&categoryId=&brandId=&status=&pageNum=&pageSize=` |
| GET/POST/DELETE | `/api/admin/robots`、`/api/admin/robots/{id}` |
| POST | `/api/admin/robots?bizType..` 保存 body 为 RobotDTO |
| POST | `/api/admin/robots/{id}/status?status=` |
| GET/POST/DELETE | `/api/admin/robots/categories`、`/api/admin/robots/categories/{id}` |
| GET/POST/DELETE | `/api/admin/robots/series?brandId=`、`/api/admin/robots/series`、`/api/admin/robots/series/{id}` |
| GET/POST/DELETE | `/api/admin/robots/templates`、`/api/admin/robots/templates/{id}`（GET 单条返回 `{template, groups:[{group, defs:[{def}]}]}`）|
| POST/DELETE | `/api/admin/robots/param-groups`、`/api/admin/robots/param-groups/{id}` |
| POST/DELETE | `/api/admin/robots/param-defs`、`/api/admin/robots/param-defs/{id}` |
| GET | `/api/admin/robots/{id}/params` |
| POST | `/api/admin/robots/params` body: `{robotId, items:[{defId,value}]}` |
| GET/POST | `/api/admin/robots/{id}/images`、`/api/admin/robots/{id}/videos`、`/api/admin/robots/{id}/prices` |
| GET | `/api/admin/robots/{id}/tags` |
| POST | `/api/admin/robots/{id}/tags?tagType=scene\|dev\|ai` body: `["标签1","标签2"]` |

### 其他后台模块

| 模块 | 路径 |
|---|---|
| 品牌 | `/api/admin/brands`（GET 分页 / GET `/all` / GET-POST `/{id}` / DELETE `/{id}` / POST `/{id}/status?status=`）|
| 企业 | `/api/admin/companies`（同上，无 status 接口）|
| 资讯 | `/api/admin/articles`、`/api/admin/articles/{id}`、`/api/admin/articles/{id}/status?status=`、`/api/admin/articles/categories`、`/api/admin/articles/categories/{id}` |
| 视频 | `/api/admin/videos`、`/{id}`、`/{id}/status`、`/categories`、`/categories/{id}` |
| 教程 | `/api/admin/tutorials`、`/{id}`、`/categories`、`/categories/{id}` |
| 社区 | `/api/admin/community/circles`、`/circles/{id}`、`/posts`、`/posts/{id}/status?status=`、`/posts/{id}`、`/comments`、`/comments/{id}` |
| 询价 | `/api/admin/inquiries`、`/{id}`、`/{id}/status?status=&handleNote=`、`/{id}/records?content=&operator=`、`/status-count` |
| Banner | `/api/admin/banners?position=`、`/{id}`、POST、DELETE |
| 推荐位 | `/api/admin/recommends/positions`、`/items?positionId=`、POST `/positions`、POST `/items`、DELETE `/items/{id}` |
| 前台用户 | `/api/admin/users`、`/{id}`、POST `/{id}/status?status=` |
| 系统 | `/api/admin/system/admins`、`/admins/{id}`、`/admins/{id}/status`、`/admins/{id}/password?password=`、`/roles`、`/roles/{id}`、`/menus`、`/menus/tree`、`/permissions`、`/logs/login`、`/logs/oper`、`/logs/error`、`/dicts?dictType=`、`/configs`、`/site-config` |

后台 POST 保存接口统一返回新记录 id；列表接口返回分页结构（`/roles`、`/menus`、`/permissions`、`/dicts`、`/configs`、`/circles`、`/categories`、`/templates` 等返回数组）。
