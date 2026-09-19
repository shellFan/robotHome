import request from '@/utils/request'

export function getGrowthDashboard(days = 7) {
  return request({ url: '/admin/growth/dashboard', method: 'get', params: { days } })
}