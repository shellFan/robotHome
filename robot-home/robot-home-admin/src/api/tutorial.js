import request from '@/utils/request'

export function getTutorialPage(params) {
  return request({ url: '/admin/tutorials', method: 'get', params })
}

export function getTutorialDetail(id) {
  return request({ url: `/admin/tutorials/${id}`, method: 'get' })
}

export function saveTutorial(data) {
  return request({ url: '/admin/tutorials', method: 'post', data })
}

export function deleteTutorial(id) {
  return request({ url: `/admin/tutorials/${id}`, method: 'delete' })
}

/** 分类（返回数组） */
export function getTutorialCategories() {
  return request({ url: '/admin/tutorials/categories', method: 'get' })
}

export function saveTutorialCategory(data) {
  return request({ url: '/admin/tutorials/categories', method: 'post', data })
}

export function deleteTutorialCategory(id) {
  return request({ url: `/admin/tutorials/categories/${id}`, method: 'delete' })
}
