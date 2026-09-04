import request from '@/utils/request'

export function getUserPage(params) {
  return request({ url: '/admin/users', method: 'get', params })
}

export function getUserDetail(id) {
  return request({ url: `/admin/users/${id}`, method: 'get' })
}

/** 封禁 / 解封：status 1 正常 0 禁用 */
export function updateUserStatus(id, status) {
  return request({ url: `/admin/users/${id}/status`, method: 'post', params: { status } })
}
