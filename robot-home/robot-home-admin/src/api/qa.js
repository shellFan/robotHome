import request from '@/utils/request'

/** 问答管理 */
export function getQuestionPage(params) {
  return request({ url: '/admin/qa/questions', method: 'get', params })
}

export function getAnswerPage(params) {
  return request({ url: '/admin/qa/answers', method: 'get', params })
}

export function deleteQuestion(id) {
  return request({ url: `/admin/qa/questions/${id}`, method: 'delete' })
}

export function deleteAnswer(id) {
  return request({ url: `/admin/qa/answers/${id}`, method: 'delete' })
}