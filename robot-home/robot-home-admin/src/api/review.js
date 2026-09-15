import request from '@/utils/request'

/** 分页查询评价列表 */
export function getReviewPage(params) {
  return request({ url: '/admin/reviews', method: 'get', params })
}

/** 审核评价（status: 1通过, 2拒绝） */
export function auditReview(id, status, reason) {
  return request({
    url: `/admin/reviews/${id}/audit`,
    method: 'post',
    data: { status, reason }
  })
}

/** 官方回复评价 */
export function replyReview(id, reply) {
  return request({
    url: `/admin/reviews/${id}/reply`,
    method: 'post',
    data: { reply }
  })
}

/** 删除评价 */
export function deleteReview(id) {
  return request({ url: `/admin/reviews/${id}`, method: 'delete' })
}

/** 评价状态映射 */
export const REVIEW_STATUS_MAP = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已拒绝', type: 'danger' }
}