import request from '@/utils/request'

export function getArticlePage(params) {
  return request({ url: '/admin/articles', method: 'get', params })
}

export function getArticleDetail(id) {
  return request({ url: `/admin/articles/${id}`, method: 'get' })
}

export function saveArticle(data) {
  return request({ url: '/admin/articles', method: 'post', data })
}

export function deleteArticle(id) {
  return request({ url: `/admin/articles/${id}`, method: 'delete' })
}

export function updateArticleStatus(id, status) {
  return request({ url: `/admin/articles/${id}/status`, method: 'post', params: { status } })
}

/** 栏目（返回数组） */
export function getArticleCategories() {
  return request({ url: '/admin/articles/categories', method: 'get' })
}

export function saveArticleCategory(data) {
  return request({ url: '/admin/articles/categories', method: 'post', data })
}

export function deleteArticleCategory(id) {
  return request({ url: `/admin/articles/categories/${id}`, method: 'delete' })
}
