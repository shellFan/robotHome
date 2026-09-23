import request from '@/utils/request'

/** 质量评分管理 */

/** Phase11: 批量计算所有机器人质量评分 */
export function computeAllQuality() {
  return request({ url: '/quality/admin/compute-all', method: 'post' })
}

/** 计算单个机器人质量评分 */
export function computeQuality(robotId) {
  return request({ url: `/quality/admin/compute/${robotId}`, method: 'post' })
}

export function getScorePage(params) {
  return request({ url: '/quality/admin/scores', method: 'get', params })
}

export function getIssuePage(params) {
  return request({ url: '/quality/admin/issues', method: 'get', params })
}

export function updateIssueStatus(id, status) {
  return request({ url: `/quality/admin/issues/${id}/status`, method: 'post', params: { status } })
}