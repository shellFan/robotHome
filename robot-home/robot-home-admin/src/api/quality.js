import request from '@/utils/request'

/** 质量评分管理 — 管理端路径已改为 /api/admin/quality/** */

/** Phase11: 批量计算所有机器人质量评分 */
export function computeAllQuality() {
  return request({ url: '/admin/quality/compute-all', method: 'post' })
}

/** 计算单个机器人质量评分 */
export function computeQuality(robotId) {
  return request({ url: `/admin/quality/compute/${robotId}`, method: 'post' })
}

export function getScorePage(params) {
  return request({ url: '/admin/quality/scores', method: 'get', params })
}

export function getIssuePage(params) {
  return request({ url: '/admin/quality/issues', method: 'get', params })
}

export function updateIssueStatus(id, status) {
  return request({ url: `/admin/quality/issues/${id}/status`, method: 'put', params: { status } })
}