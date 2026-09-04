import request from '@/utils/request'

export function getBrandPage(params) {
  return request({ url: '/admin/brands', method: 'get', params })
}

/** 全部启用品牌（下拉用，返回数组） */
export function getBrandAll() {
  return request({ url: '/admin/brands/all', method: 'get' })
}

export function getBrandDetail(id) {
  return request({ url: `/admin/brands/${id}`, method: 'get' })
}

export function saveBrand(data) {
  return request({ url: '/admin/brands', method: 'post', data })
}

export function deleteBrand(id) {
  return request({ url: `/admin/brands/${id}`, method: 'delete' })
}

export function updateBrandStatus(id, status) {
  return request({ url: `/admin/brands/${id}/status`, method: 'post', params: { status } })
}
