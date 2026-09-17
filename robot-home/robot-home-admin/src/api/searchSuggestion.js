import request from '@/utils/request'

export function getSuggestionPage(params) {
  return request({ url: '/admin/search-suggestions', method: 'get', params })
}

export function getSuggestionDetail(id) {
  return request({ url: `/admin/search-suggestions/${id}`, method: 'get' })
}

export function addSuggestion(data) {
  return request({ url: '/admin/search-suggestions', method: 'post', data })
}

export function updateSuggestion(id, data) {
  return request({ url: `/admin/search-suggestions/${id}`, method: 'put', data })
}

export function deleteSuggestion(id) {
  return request({ url: `/admin/search-suggestions/${id}`, method: 'delete' })
}

export function toggleSuggestionEnabled(id) {
  return request({ url: `/admin/search-suggestions/${id}/toggle-enabled`, method: 'post' })
}

export const SUGGESTION_TYPE_MAP = {
  keyword: { label: '关键词', color: '#1668dc' },
  brand: { label: '品牌', color: '#d9822b' },
  category: { label: '分类', color: '#2ba471' }
}