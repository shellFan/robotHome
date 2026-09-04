const { robotApi } = require('../../api/index')
const { formatPrice, imageOf, parseMainParams } = require('../../utils/format')
const { getCompareIds, setCompareIds, clearCompare, toggleCompare } = require('../../utils/compare')

Page({
  data: { robots: [], loading: false, empty: false },
  onShow() { this.load() },
  async load() {
    const ids = getCompareIds()
    if (!ids.length) {
      this.setData({ robots: [], empty: true })
      return
    }
    this.setData({ loading: true, empty: false })
    try {
      const data = await robotApi.compare(ids.join(','))
      const list = (Array.isArray(data) ? data : (data && data.robots) || []).map(function (r) {
        return Object.assign({}, r, {
          cover: imageOf(r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice),
          params: parseMainParams(r.mainParams, 8)
        })
      })
      this.setData({ robots: list, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  remove(e) {
    toggleCompare(e.currentTarget.dataset.id)
    this.load()
  },
  clearAll() {
    clearCompare()
    this.setData({ robots: [], empty: true })
  },
  goAdd() { wx.switchTab({ url: '/pages/robots/list' }) },
  goDetail(e) { wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id }) }
})
