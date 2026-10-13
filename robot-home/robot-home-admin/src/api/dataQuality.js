import request from '@/utils/request'

/** Phase12: 数据质量详细统计 — Coverage/Freshness/Source Health */
export function getDataQuality() {
  return request({ url: '/admin/dashboard/data-quality', method: 'get' })
}

/** Phase12: 刷新所有Robot的Freshness新鲜度 */
export function refreshFreshness() {
  return request({ url: '/admin/dashboard/refresh-freshness', method: 'post' })
}

/** Phase12 P0-5: Coverage下钻统计 — 按dataSource/brand/category维度 */
export function getCoverageDrillDown(dimension) {
  return request({ url: '/admin/dashboard/coverage-drill-down', method: 'get', params: { dimension } })
}

/** Phase12 P0-6: 数据Scorecard — 捕获并持久化快照(不可变) */
export function captureScorecard(label) {
  return request({ url: '/admin/dashboard/scorecard', method: 'post', params: { label } })
}

/** Phase12 P0-6: 读取最近一次指定label的Scorecard快照(不可变) */
export function getScorecard(label) {
  return request({ url: '/admin/dashboard/scorecard', method: 'get', params: { label } })
}