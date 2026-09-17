import request from '@/utils/request'

/** 搜索别名管理 */
export function getAliasPage(params) {
  return request({ url: '/admin/search/aliases', method: 'get', params })
}

export function createAlias(data) {
  return request({ url: '/admin/search/aliases', method: 'post', data })
}

export function updateAlias(id, data) {
  return request({ url: `/admin/search/aliases/${id}`, method: 'put', data })
}

export function toggleAliasStatus(id) {
  return request({ url: `/admin/search/aliases/${id}/toggle`, method: 'post' })
}

/** 零结果搜索管理 */
export function getZeroResultPage(params) {
  return request({ url: '/admin/search/zero-results', method: 'get', params })
}

export function updateZeroResultAction(id, action) {
  return request({ url: `/admin/search/zero-results/${id}/action`, method: 'post', params: { action } })
}