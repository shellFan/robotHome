import request from '@/utils/request'

// ---------------- 管理员 ----------------
export function getAdminPage(params) {
  return request({ url: '/admin/system/admins', method: 'get', params })
}

export function getAdminDetail(id) {
  return request({ url: `/admin/system/admins/${id}`, method: 'get' })
}

export function saveAdmin(data) {
  return request({ url: '/admin/system/admins', method: 'post', data })
}

export function deleteAdmin(id) {
  return request({ url: `/admin/system/admins/${id}`, method: 'delete' })
}

export function updateAdminStatus(id, status) {
  return request({ url: `/admin/system/admins/${id}/status`, method: 'post', params: { status } })
}

export function resetAdminPassword(id, password) {
  return request({
    url: `/admin/system/admins/${id}/password`,
    method: 'post',
    params: { password }
  })
}

// ---------------- 角色（返回数组） ----------------
export function getRoles() {
  return request({ url: '/admin/system/roles', method: 'get' })
}

export function getRoleDetail(id) {
  return request({ url: `/admin/system/roles/${id}`, method: 'get' })
}

export function saveRole(data) {
  return request({ url: '/admin/system/roles', method: 'post', data })
}

export function deleteRole(id) {
  return request({ url: `/admin/system/roles/${id}`, method: 'delete' })
}

// ---------------- 菜单（返回数组） ----------------
export function getMenus() {
  return request({ url: '/admin/system/menus', method: 'get' })
}

export function getMenuTree() {
  return request({ url: '/admin/system/menus/tree', method: 'get' })
}

export function saveMenu(data) {
  return request({ url: '/admin/system/menus', method: 'post', data })
}

export function deleteMenu(id) {
  return request({ url: `/admin/system/menus/${id}`, method: 'delete' })
}

// ---------------- 权限（返回数组） ----------------
export function getPermissions() {
  return request({ url: '/admin/system/permissions', method: 'get' })
}

export function savePermission(data) {
  return request({ url: '/admin/system/permissions', method: 'post', data })
}

export function deletePermission(id) {
  return request({ url: `/admin/system/permissions/${id}`, method: 'delete' })
}

// ---------------- 日志 ----------------
export function getLoginLogs(params) {
  return request({ url: '/admin/system/logs/login', method: 'get', params })
}

export function getOperLogs(params) {
  return request({ url: '/admin/system/logs/oper', method: 'get', params })
}

export function getErrorLogs(params) {
  return request({ url: '/admin/system/logs/error', method: 'get', params })
}

// ---------------- 字典（返回数组） ----------------
export function getDicts(dictType) {
  return request({ url: '/admin/system/dicts', method: 'get', params: { dictType } })
}

export function saveDict(data) {
  return request({ url: '/admin/system/dicts', method: 'post', data })
}

export function deleteDict(id) {
  return request({ url: `/admin/system/dicts/${id}`, method: 'delete' })
}

// ---------------- 配置（返回数组） ----------------
export function getConfigs() {
  return request({ url: '/admin/system/configs', method: 'get' })
}

export function saveConfig(data) {
  return request({ url: '/admin/system/configs', method: 'post', data })
}

export function deleteConfig(id) {
  return request({ url: `/admin/system/configs/${id}`, method: 'delete' })
}
