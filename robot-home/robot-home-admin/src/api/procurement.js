import request from '@/utils/request'

/** 采购管理 */
export function getProcurementPage(params) {
  return request({ url: '/admin/procurement/list', method: 'get', params })
}

export function assignProcurement(id, assignedTo) {
  return request({ url: `/admin/procurement/${id}/assign`, method: 'post', params: { assignedTo } })
}

export function updateLeadScore(id, score) {
  return request({ url: `/admin/procurement/${id}/lead-score`, method: 'post', params: { score } })
}