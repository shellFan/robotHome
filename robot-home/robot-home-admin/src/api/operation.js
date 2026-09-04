import request from '@/utils/request'

// ---------------- Banner ----------------
export function getBannerList(position) {
  return request({ url: '/admin/banners', method: 'get', params: { position } })
}

export function saveBanner(data) {
  return request({ url: '/admin/banners', method: 'post', data })
}

export function deleteBanner(id) {
  return request({ url: `/admin/banners/${id}`, method: 'delete' })
}

// ---------------- 推荐位 ----------------
export function getRecommendPositions() {
  return request({ url: '/admin/recommends/positions', method: 'get' })
}

export function saveRecommendPosition(data) {
  return request({ url: '/admin/recommends/positions', method: 'post', data })
}

export function getRecommendItems(positionId) {
  return request({ url: '/admin/recommends/items', method: 'get', params: { positionId } })
}

export function saveRecommendItem(data) {
  return request({ url: '/admin/recommends/items', method: 'post', data })
}

export function deleteRecommendItem(id) {
  return request({ url: `/admin/recommends/items/${id}`, method: 'delete' })
}
