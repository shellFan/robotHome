import request from '@/utils/request'

/** 分页查询纠错列表 */
export function getCorrectionPage(params) {
  return request({ url: '/admin/corrections', method: 'get', params })
}

/** 审核纠错（status: 1采纳, 2拒绝） */
export function auditCorrection(id, status, reason) {
  return request({
    url: `/admin/corrections/${id}/audit`,
    method: 'post',
    data: { status, reason }
  })
}

/** 删除纠错 */
export function deleteCorrection(id) {
  return request({ url: `/admin/corrections/${id}`, method: 'delete' })
}

/** 纠错状态映射 */
export const CORRECTION_STATUS_MAP = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已采纳', type: 'success' },
  2: { label: '已拒绝', type: 'danger' }
}