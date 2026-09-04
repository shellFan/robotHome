const { favoriteApi, userApi } = require('../../api/index')
const { formatPrice, imageOf, unwrapList } = require('../../utils/format')
Page({
  data: { list: [], page: 1, finished: false, loading: false, bizType: 'robot' },
  onLoad() { this.reload() },
  onReachBottom() { this.more() },
  switchType(e) {
    this.setData({ bizType: e.currentTarget.dataset.type })
    this.reload()
  },
  async reload() { this.setData({ page: 1, list: [], finished: false }); await this.fetch() },
  async more() {
    if (this.data.loading || this.data.finished) return
    this.setData({ page: this.data.page + 1 })
    await this.fetch(true)
  },
  async fetch(append) {
    this.setData({ loading: true })
    try {
      let data
      try { data = await favoriteApi.list({ page: this.data.page, size: 10, bizType: this.data.bizType }) }
      catch (e) { data = await userApi.myFavorites({ page: this.data.page, size: 10, bizType: this.data.bizType }) }
      const records = unwrapList(data).map(function (item) {
        const target = item.target || item.robot || item.article || item
        return {
          id: item.bizId || target.id,
          bizType: item.bizType || 'robot',
          title: target.name || target.title || '未命名',
          cover: imageOf(target.cover || target.mainImage || target.logo),
          priceText: formatPrice(target.guidePrice)
        }
      })
      const list = append ? this.data.list.concat(records) : records
      this.setData({ list: list, finished: records.length < 10, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  goDetail(e) {
    const item = e.currentTarget.dataset.item
    if (item.bizType === 'article') wx.navigateTo({ url: '/pages/articles/detail?id=' + item.id })
    else if (item.bizType === 'video') wx.navigateTo({ url: '/pages/videos/detail?id=' + item.id })
    else wx.navigateTo({ url: '/pages/robots/detail?id=' + item.id })
  }
})
