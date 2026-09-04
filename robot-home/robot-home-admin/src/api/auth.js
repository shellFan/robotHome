import request from '@/utils/request'

/** 后台登录（Spring @RequestParam，使用 query 传参） */
export function login(data) {
  return request({
    url: '/admin/auth/login',
    method: 'post',
    params: { username: data.username, password: data.password }
  })
}

/** 管理员信息 + roles + permissions + menus(树) */
export function getInfo() {
  return request({ url: '/admin/auth/info', method: 'get' })
}

export function logout() {
  return request({ url: '/admin/auth/logout', method: 'post' })
}
