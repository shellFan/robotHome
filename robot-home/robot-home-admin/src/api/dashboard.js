import request from '@/utils/request'

export function getStats() {
  return request({ url: '/admin/dashboard/stats', method: 'get' })
}

export function getTrend(days = 7) {
  return request({ url: '/admin/dashboard/trend', method: 'get', params: { days } })
}

/** Phase11: 采集器健康状态 */
export function getCrawlerHealth() {
  return request({ url: '/admin/dashboard/crawler-health', method: 'get' })
}

/** Phase11: 数据质量概览 */
export function getQualitySummary() {
  return request({ url: '/admin/dashboard/quality-summary', method: 'get' })
}
