import request from '@/utils/request'

// ==================== 套餐管理 ====================

export function listPlans(params) {
  return request({ url: '/admin/commercial/plans', method: 'get', params })
}

export function savePlan(data) {
  return request({ url: '/admin/commercial/plans', method: 'post', data })
}

export function updatePlanStatus(planId, status) {
  return request({ url: `/admin/commercial/plans/${planId}/status`, method: 'post', params: { status } })
}

export function createPlanVersion(planId, data) {
  return request({ url: `/admin/commercial/plans/${planId}/versions`, method: 'post', params: data })
}

export function listPlanVersions(planId) {
  return request({ url: `/admin/commercial/plans/${planId}/versions`, method: 'get' })
}

// ==================== 订单管理 ====================

export function listOrders(params) {
  return request({ url: '/admin/commercial/orders', method: 'get', params })
}

export function getOrder(orderId) {
  return request({ url: `/admin/commercial/orders/${orderId}`, method: 'get' })
}

// ==================== 退款管理 ====================

export function listRefunds(params) {
  return request({ url: '/admin/commercial/refunds', method: 'get', params })
}

export function applyRefund(orderId, reason) {
  return request({ url: '/admin/commercial/refunds', method: 'post', params: { orderId, reason } })
}

export function confirmRefund(refundId) {
  return request({ url: `/admin/commercial/refunds/${refundId}/confirm`, method: 'post' })
}

export function failRefund(refundId, reason) {
  return request({ url: `/admin/commercial/refunds/${refundId}/fail`, method: 'post', params: { reason } })
}

// ==================== 商业统计 ====================

export function getCommercialStats() {
  return request({ url: '/admin/commercial/stats', method: 'get' })
}

// ==================== 订阅管理 ====================

export function getSubscription(companyId) {
  return request({ url: `/admin/commercial/subscriptions/${companyId}`, method: 'get' })
}

export function suspendSubscription(companyId, reason) {
  return request({ url: `/admin/commercial/subscriptions/${companyId}/suspend`, method: 'post', params: { reason } })
}

export function resumeSubscription(companyId) {
  return request({ url: `/admin/commercial/subscriptions/${companyId}/resume`, method: 'post' })
}