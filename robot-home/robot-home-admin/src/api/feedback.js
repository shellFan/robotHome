import request from '@/utils/request'

export function getFeedbackPage(params) {
  return request({ url: '/admin/feedbacks', method: 'get', params })
}

export function getFeedbackDetail(id) {
  return request({ url: `/admin/feedbacks/${id}`, method: 'get' })
}

export function handleFeedback(id, handleNote) {
  return request({
    url: `/admin/feedbacks/${id}/handle`,
    method: 'post',
    params: { handleNote }
  })
}

export function deleteFeedback(id) {
  return request({ url: `/admin/feedbacks/${id}`, method: 'delete' })
}

export const FEEDBACK_TYPE_MAP = {
  bug: { label: 'Bug 反馈', type: 'danger' },
  feature: { label: '功能建议', type: 'primary' },
  improvement: { label: '体验改进', type: 'warning' },
  other: { label: '其他', type: 'info' }
}

export const FEEDBACK_STATUS_MAP = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '已处理', type: 'success' }
}