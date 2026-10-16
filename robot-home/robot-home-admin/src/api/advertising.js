import request from '@/utils/request'

// ==================== 广告位管理 ====================

export function listAllSlots() {
  return request({ url: '/admin/advertising/slots', method: 'get' })
}

export function saveSlot(data) {
  return request({ url: '/admin/advertising/slots', method: 'post', data })
}

export function updateSlotStatus(slotId, status) {
  return request({ url: `/admin/advertising/slots/${slotId}/status`, method: 'post', params: { status } })
}

export function getSlotStats(slotId) {
  return request({ url: `/admin/advertising/slots/${slotId}/stats`, method: 'get' })
}

// ==================== 广告审核 ====================

export function listCampaigns(params) {
  return request({ url: '/admin/advertising/campaigns', method: 'get', params })
}

export function approveCampaign(campaignId, note) {
  return request({ url: `/admin/advertising/campaigns/${campaignId}/approve`, method: 'post', params: { note } })
}

export function rejectCampaign(campaignId, reason) {
  return request({ url: `/admin/advertising/campaigns/${campaignId}/reject`, method: 'post', params: { reason } })
}

// ==================== 商机推荐 ====================

export function batchGenerateRecommendations(perCompanyLimit = 5) {
  return request({ url: '/admin/advertising/lead-recommendations/batch-generate', method: 'post', params: { perCompanyLimit } })
}

export function listRecommendations(params) {
  return request({ url: '/admin/advertising/lead-recommendations', method: 'get', params })
}