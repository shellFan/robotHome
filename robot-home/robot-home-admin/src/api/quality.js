import request from '@/utils/request'

/** 质量评分管理 */
export function computeQuality() {
  return request({ url: '/quality/admin/compute', method: 'post' })
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