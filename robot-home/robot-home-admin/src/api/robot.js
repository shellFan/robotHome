import request from '@/utils/request'

// ---------------- 型号 ----------------
export function getRobotPage(params) {
  return request({ url: '/admin/robots', method: 'get', params })
}

export function getRobotDetail(id) {
  return request({ url: `/admin/robots/${id}`, method: 'get' })
}

export function saveRobot(data) {
  return request({ url: '/admin/robots', method: 'post', data })
}

export function deleteRobot(id) {
  return request({ url: `/admin/robots/${id}`, method: 'delete' })
}

export function updateRobotStatus(id, status) {
  return request({ url: `/admin/robots/${id}/status`, method: 'post', params: { status } })
}

// ---------------- 分类（返回数组） ----------------
export function getRobotCategories() {
  return request({ url: '/admin/robots/categories', method: 'get' })
}

export function saveRobotCategory(data) {
  return request({ url: '/admin/robots/categories', method: 'post', data })
}

export function deleteRobotCategory(id) {
  return request({ url: `/admin/robots/categories/${id}`, method: 'delete' })
}

// ---------------- 系列（返回数组） ----------------
export function getRobotSeries(brandId) {
  return request({ url: '/admin/robots/series', method: 'get', params: { brandId } })
}

export function saveRobotSeries(data) {
  return request({ url: '/admin/robots/series', method: 'post', data })
}

export function deleteRobotSeries(id) {
  return request({ url: `/admin/robots/series/${id}`, method: 'delete' })
}

// ---------------- 参数模板（返回数组） ----------------
export function getRobotTemplates() {
  return request({ url: '/admin/robots/templates', method: 'get' })
}

export function getRobotTemplateDetail(id) {
  return request({ url: `/admin/robots/templates/${id}`, method: 'get' })
}

export function saveRobotTemplate(data) {
  return request({ url: '/admin/robots/templates', method: 'post', data })
}

export function deleteRobotTemplate(id) {
  return request({ url: `/admin/robots/templates/${id}`, method: 'delete' })
}

// ---------------- 参数分组 / 参数定义 ----------------
export function saveParamGroup(data) {
  return request({ url: '/admin/robots/param-groups', method: 'post', data })
}

export function deleteParamGroup(id) {
  return request({ url: `/admin/robots/param-groups/${id}`, method: 'delete' })
}

export function saveParamDef(data) {
  return request({ url: '/admin/robots/param-defs', method: 'post', data })
}

export function deleteParamDef(id) {
  return request({ url: `/admin/robots/param-defs/${id}`, method: 'delete' })
}

// ---------------- 参数值 ----------------
export function getRobotParams(id) {
  return request({ url: `/admin/robots/${id}/params`, method: 'get' })
}

export function saveRobotParams(data) {
  return request({ url: '/admin/robots/params', method: 'post', data })
}

// ---------------- 图片 / 视频 / 价格 / 标签 ----------------
export function getRobotImages(id) {
  return request({ url: `/admin/robots/${id}/images`, method: 'get' })
}

export function saveRobotImages(id, data) {
  return request({ url: `/admin/robots/${id}/images`, method: 'post', data })
}

export function getRobotVideos(id) {
  return request({ url: `/admin/robots/${id}/videos`, method: 'get' })
}

export function saveRobotVideos(id, data) {
  return request({ url: `/admin/robots/${id}/videos`, method: 'post', data })
}

export function getRobotPrices(id) {
  return request({ url: `/admin/robots/${id}/prices`, method: 'get' })
}

export function saveRobotPrices(id, data) {
  return request({ url: `/admin/robots/${id}/prices`, method: 'post', data })
}

export function getRobotTags(id) {
  return request({ url: `/admin/robots/${id}/tags`, method: 'get' })
}

export function saveRobotTags(id, tagType, values) {
  return request({
    url: `/admin/robots/${id}/tags`,
    method: 'post',
    params: { tagType },
    data: values || []
  })
}
