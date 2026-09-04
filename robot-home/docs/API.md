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
