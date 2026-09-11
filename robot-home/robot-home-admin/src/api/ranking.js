import request from '@/utils/request'

// 排名权重
export function getRankingWeights() {
  return request({ url: '/admin/ranking/weights', method: 'get' })
}

export function addRankingWeight(data) {
  return request({ url: '/admin/ranking/weights', method: 'post', data })
}

export function updateRankingWeight(id, data) {
  return request({ url: `/admin/ranking/weights/${id}`, method: 'put', data })
}

// 衰减配置
export function getDecayConfigs() {
  return request({ url: '/admin/ranking/decay-configs', method: 'get' })
}

export function updateDecayConfig(id, data) {
  return request({ url: `/admin/ranking/decay-configs/${id}`, method: 'put', data })
}

// 快照
export function getRankingSnapshots(params) {
  return request({ url: '/admin/ranking/snapshots', method: 'get', params })
}

// 操作
export function refreshRanking() {
  return request({ url: '/admin/ranking/refresh', method: 'post' })
}

export function createRankingSnapshot() {
  return request({ url: '/admin/ranking/snapshot', method: 'post' })
}

export function getCurrentWeights() {
  return request({ url: '/admin/ranking/current-weights', method: 'get' })
}