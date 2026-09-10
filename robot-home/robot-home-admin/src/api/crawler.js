import request from '@/utils/request'

// ==================== 数据源 ====================

export function getCrawlerSourcePage(params) {
  return request({ url: '/admin/crawler/source/list', method: 'get', params })
}

export function getCrawlerSource(id) {
  return request({ url: `/admin/crawler/source/${id}`, method: 'get' })
}

export function createCrawlerSource(data) {
  return request({ url: '/admin/crawler/source', method: 'post', data })
}

export function updateCrawlerSource(id, data) {
  return request({ url: `/admin/crawler/source/${id}`, method: 'put', data })
}

export function deleteCrawlerSource(id) {
  return request({ url: `/admin/crawler/source/${id}`, method: 'delete' })
}

export function toggleCrawlerSource(id) {
  return request({ url: `/admin/crawler/source/${id}/toggle`, method: 'put' })
}

export function getActiveCrawlerSources() {
  return request({ url: '/admin/crawler/source/active', method: 'get' })
}

export function clearCrawlerDedup() {
  return request({ url: '/admin/crawler/source/clear-dedup', method: 'post' })
}

// ==================== 采集任务 ====================

export function getCrawlerTaskPage(params) {
  return request({ url: '/admin/crawler/task/list', method: 'get', params })
}

export function getCrawlerTask(id) {
  return request({ url: `/admin/crawler/task/${id}`, method: 'get' })
}

export function createCrawlerTask(data) {
  return request({ url: '/admin/crawler/task', method: 'post', data })
}

export function startCrawlerTask(id) {
  return request({ url: `/admin/crawler/task/${id}/start`, method: 'post' })
}

export function stopCrawlerTask(id) {
  return request({ url: `/admin/crawler/task/${id}/stop`, method: 'post' })
}

export function deleteCrawlerTask(id) {
  return request({ url: `/admin/crawler/task/${id}`, method: 'delete' })
}

// ==================== 内容审核 ====================

export function getCrawlerArticlePage(params) {
  return request({ url: '/admin/crawler/content/article/list', method: 'get', params })
}

export function getCrawlerArticle(id) {
  return request({ url: `/admin/crawler/content/article/${id}`, method: 'get' })
}

/**
 * 审核文章 — 后端 @RequestParam action=approve/reject, reason=xxx
 */
export function reviewCrawlerArticle(id, action, reason) {
  return request({ url: `/admin/crawler/content/article/${id}/review`, method: 'put', params: { action, reason } })
}

/**
 * 批量审核文章 — 后端 @RequestBody List<Long> ids + @RequestParam action
 */
export function batchReviewCrawlerArticles(ids, action) {
  return request({ url: '/admin/crawler/content/article/batch-review', method: 'put', data: ids, params: { action } })
}

export function getCrawlerProductPage(params) {
  return request({ url: '/admin/crawler/content/product/list', method: 'get', params })
}

export function getCrawlerProduct(id) {
  return request({ url: `/admin/crawler/content/product/${id}`, method: 'get' })
}

/**
 * 审核产品 — 后端 @RequestParam action=approve/reject, reason=xxx
 */
export function reviewCrawlerProduct(id, action, reason) {
  return request({ url: `/admin/crawler/content/product/${id}/review`, method: 'put', params: { action, reason } })
}

/**
 * 批量审核产品 — 后端 @RequestBody List<Long> ids + @RequestParam action
 */
export function batchReviewCrawlerProducts(ids, action) {
  return request({ url: '/admin/crawler/content/product/batch-review', method: 'put', data: ids, params: { action } })
}

export function getCrawlerContentStats() {
  return request({ url: '/admin/crawler/content/stats', method: 'get' })
}

// ==================== 发布管理 ====================

export function getCrawlerPublishStats() {
  return request({ url: '/admin/crawler/publish/stats', method: 'get' })
}

export function publishCrawlerArticles() {
  return request({ url: '/admin/crawler/publish/articles', method: 'post' })
}

export function publishCrawlerProducts() {
  return request({ url: '/admin/crawler/publish/products', method: 'post' })
}

export function publishCrawlerAll() {
  return request({ url: '/admin/crawler/publish/all', method: 'post' })
}