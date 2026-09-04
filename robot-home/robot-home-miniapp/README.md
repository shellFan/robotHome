# 机器人之家 · 微信小程序

原生微信小程序（JavaScript），对接与 PC 端相同的后端 API（默认 `http://localhost:8081/api`）。

## 用微信开发者工具打开

1. 安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 选择「导入项目」→ 目录选本文件夹：
   `robot-home/robot-home-miniapp`
3. AppID 可使用「测试号」或「游客模式」（`project.config.json` 中为 `touristappid`）
4. 详情 → 本地设置：勾选 **不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书**（开发期必需）
5. 确保后端 `robot-home-server` 已在 **8081** 端口启动

### 真机预览

将 `utils/config.js` 中的 `baseUrl` 改为电脑局域网 IP，例如：

```js
baseUrl: 'http://192.168.1.8:8081/api'
```

并保证手机与电脑同一局域网。

## TabBar

| Tab | 页面 |
|-----|------|
| 首页 | `pages/index/index` |
| 选机器人 | `pages/robots/list` |
| 发现 | `pages/discover/index` |
| 社区 | `pages/community/index` |
| 我的 | `pages/mine/index` |

## 登录说明

无真实 AppID 时，打开「我的 → 登录 → 开发登录」，填写 `dev_openid_xxx` 调用 `/auth/wx-login`（表单参数，与后端 `@RequestParam` 一致）。也可使用账号密码 / 短信登录。

Token 存储在 `wx.storage` 的 `rh_token`，请求头为 `Authorization: Bearer <token>`。
