import request from '@/utils/request'

export function getVideoPage(params) {
  return request({ url: '/admin/videos', method: 'get', params })
}

export function getVideoDetail(id) {
  return request({ url: `/admin/videos/${id}`, method: 'get' })
}

export function saveVideo(data) {
  return request({ url: '/admin/videos', method: 'post', data })
}

export function deleteVideo(id) {
  return request({ url: `/admin/videos/${id}`, method: 'delete' })
}

export function updateVideoStatus(id, status) {
  return request({ url: `/admin/videos/${id}/status`, method: 'post', params: { status } })
}

/** 频道（返回数组） */
export function getVideoCategories() {
  return request({ url: '/admin/videos/categories', method: 'get' })
}

export function saveVideoCategory(data) {
  return request({ url: '/admin/videos/categories', method: 'post', data })
}

export function deleteVideoCategory(id) {
  return request({ url: `/admin/videos/categories/${id}`, method: 'delete' })
}
