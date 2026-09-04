const { historyApi, userApi } = require('../../api/index')
const { formatPrice, imageOf, unwrapList, fromNow } = require('../../utils/format')
Page({
  data: { list: [], page: 1, finished: false, loading: false },
  onLoad() { this.reload() },
  onReachBottom() { this.more() },
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
      try { data = await historyApi.list({ page: this.data.page, size: 20 }) }
      catch (e) { data = await userApi.myHistory({ page: this.data.page, size: 20 }) }
      const records = unwrapList(data).map(function (item) {
        const target = item.target || item.robot || item
        return {
          id: item.bizId || target.id,
          bizType: item.bizType || 'robot',
          title: target.name || target.title || '未命名',
          cover: imageOf(target.cover || target.mainImage),
          priceText: formatPrice(target.guidePrice),
          timeText: fromNow(item.createTime || item.viewTime)
        }
      })
      const list = append ? this.data.list.concat(records) : records
      this.setData({ list: list, finished: records.length < 20, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  async clearAll() {
    try {
      await historyApi.clear()
      this.reload()
    } catch (e) {}
  },
  goDetail(e) {
    const item = e.currentTarget.dataset.item
    if (item.bizType === 'article') wx.navigateTo({ url: '/pages/articles/detail?id=' + item.id })
    else wx.navigateTo({ url: '/pages/robots/detail?id=' + item.id })
  }
})
