import request from '@/utils/request'

/** 圈子（返回数组） */
export function getCircles() {
  return request({ url: '/admin/community/circles', method: 'get' })
}

export function saveCircle(data) {
  return request({ url: '/admin/community/circles', method: 'post', data })
}

export function deleteCircle(id) {
  return request({ url: `/admin/community/circles/${id}`, method: 'delete' })
}

/** 帖子 */
export function getPostPage(params) {
  return request({ url: '/admin/community/posts', method: 'get', params })
}

/** 帖子审核：0待审 1正常 2下架 */
export function auditPost(id, status) {
  return request({ url: `/admin/community/posts/${id}/status`, method: 'post', params: { status } })
}

export function deletePost(id) {
  return request({ url: `/admin/community/posts/${id}`, method: 'delete' })
}

/** 评论 */
export function getCommentPage(params) {
  return request({ url: '/admin/community/comments', method: 'get', params })
}

export function deleteComment(id) {
  return request({ url: `/admin/community/comments/${id}`, method: 'delete' })
}
