import request from '@/utils/request'

// ==================== 数据源 ====================

export function getCrawlerSourcePage(params) {
  return request({ url: '/crawler/source/list', method: 'get', params })
}

export function getCrawlerSource(id) {
  return request({ url: `/crawler/source/${id}`, method: 'get' })
}

export function createCrawlerSource(data) {
  return request({ url: '/crawler/source', method: 'post', data })
}

export function updateCrawlerSource(id, data) {
  return request({ url: `/crawler/source/${id}`, method: 'put', data })
}

export function deleteCrawlerSource(id) {
  return request({ url: `/crawler/source/${id}`, method: 'delete' })
}

export function toggleCrawlerSource(id) {
  return request({ url: `/crawler/source/${id}/toggle`, method: 'put' })
}

export function getActiveCrawlerSources() {
  return request({ url: '/crawler/source/active', method: 'get' })
}

export function clearCrawlerDedup() {
  return request({ url: '/crawler/source/clear-dedup', method: 'post' })
}

// ==================== 采集任务 ====================

export function getCrawlerTaskPage(params) {
  return request({ url: '/crawler/task/list', method: 'get', params })
}

export function getCrawlerTask(id) {
  return request({ url: `/crawler/task/${id}`, method: 'get' })
}

export function createCrawlerTask(data) {
  return request({ url: '/crawler/task', method: 'post', data })
}

export function startCrawlerTask(id) {
  return request({ url: `/crawler/task/${id}/start`, method: 'post' })
}

export function stopCrawlerTask(id) {
  return request({ url: `/crawler/task/${id}/stop`, method: 'post' })
}

export function deleteCrawlerTask(id) {
  return request({ url: `/crawler/task/${id}`, method: 'delete' })
}

// ==================== 内容审核 ====================

export function getCrawlerArticlePage(params) {
  return request({ url: '/crawler/content/article/list', method: 'get', params })
}

export function getCrawlerArticle(id) {
  return request({ url: `/crawler/content/article/${id}`, method: 'get' })
}

export function reviewCrawlerArticle(id, data) {
  return request({ url: `/crawler/content/article/${id}/review`, method: 'put', data })
}

export function batchReviewCrawlerArticles(data) {
  return request({ url: '/crawler/content/article/batch-review', method: 'put', data })
}

export function getCrawlerProductPage(params) {
  return request({ url: '/crawler/content/product/list', method: 'get', params })
}

export function getCrawlerProduct(id) {
  return request({ url: `/crawler/content/product/${id}`, method: 'get' })
}

export function reviewCrawlerProduct(id, data) {
  return request({ url: `/crawler/content/product/${id}/review`, method: 'put', data })
}

export function batchReviewCrawlerProducts(data) {
  return request({ url: '/crawler/content/product/batch-review', method: 'put', data })
}

export function getCrawlerContentStats() {
  return request({ url: '/crawler/content/stats', method: 'get' })
}