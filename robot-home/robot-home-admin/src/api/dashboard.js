import request from '@/utils/request'

export function getStats() {
  return request({ url: '/admin/dashboard/stats', method: 'get' })
}

export function getTrend(days = 7) {
  return request({ url: '/admin/dashboard/trend', method: 'get', params: { days } })
}
