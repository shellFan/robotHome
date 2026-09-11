import request from '@/utils/request'

export function getInquiryPage(params) {
  return request({ url: '/admin/inquiries', method: 'get', params })
}

export function getInquiryDetail(id) {
  return request({ url: `/admin/inquiries/${id}`, method: 'get' })
}

export function updateInquiryStatus(id, status, handleNote) {
  return request({
    url: `/admin/inquiries/${id}/status`,
    method: 'post',
    params: { status, handleNote }
  })
}

export function addInquiryRecord(id, content, operator) {
  return request({
    url: `/admin/inquiries/${id}/records`,
    method: 'post',
    params: { content, operator }
  })
}

export function deleteInquiry(id) {
  return request({ url: `/admin/inquiries/${id}`, method: 'delete' })
}

/** 各状态数量：{ "1": n, "2": n, ... } */
export function getInquiryStatusCount() {
  return request({ url: '/admin/inquiries/status-count', method: 'get' })
}

export const INQUIRY_STATUS_MAP = {
  1: { label: '待处理', type: 'warning' },
  2: { label: '处理中', type: 'primary' },
  3: { label: '已联系', type: 'info' },
  4: { label: '已成交', type: 'success' },
  5: { label: '已关闭', type: 'info' },
  6: { label: '无效', type: 'danger' }
}

export const CUSTOMER_TYPE_MAP = {
  1: '个人',
  2: '企业',
  3: '经销商',
  4: '科研院校'
}
