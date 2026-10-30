# Phase16.3 资讯分类与质量报告

## 线上只读结果

2026-10-30 当前公开 API 返回资讯 37 条、分类 12 项。逐项分类计数均为 0；资讯记录分类字段非空数为 0。分类 API 和资讯 API 返回 HTTP 200。按线上返回的 12 个 categoryId 逐个请求筛选，12/12 均业务成功且总数为 0（日志：`docs/phase16.3/logs/category-filter-phase163-current.log`）。

只读快照 docs/phase16.3/data/news-live-readonly.csv 有 37 条：37 个标题含 nuxt-app 模板文本，17 条摘要包含 Cookie/隐私类文字，16 条封面指向官网 logo.svg，15 条没有封面。分类 dry-run v2 对 37 条全部标记 MANUAL_REVIEW_REQUIRED；不将标题关键词作为自动发布或推荐的依据。

## 处理结果

- 生产分类变更：0。
- 文章删除/下架：0。
- 作者、来源和人工编辑内容：未变更。
- dry-run：已生成，位于 docs/phase16.3/data/news-classification-dry-run-v2.csv。
- 未能可靠判断的记录：全部保留待人工审核。

由于公开 API 未暴露来源、审核状态和正文全文，当前不能据此安全地修复线上文章。Cookie 正文、模板标题、Logo 封面应逐条对照官方原文；疑似产品页需进入产品审核流程，不能直接删除。生产备份和授权前不写入。
