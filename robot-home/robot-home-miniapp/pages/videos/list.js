const api = require('../../api/index').videoApi
const { formatDate, formatCount, formatDuration, imageOf, unwrapList } = require('../../utils/format')
Page({
  data: { list: [], page: 1, finished: false, loading: false, keyword: '' },
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
      const data = await api.page({ page: this.data.page, size: 10, keyword: this.data.keyword || undefined })
      const records = unwrapList(data).map(function (item) {
        return Object.assign({}, item, {
          cover: imageOf(item.cover),
          dateText: formatDate(item.publishTime || item.createTime),
          viewText: formatCount(item.viewCount),
          durationText: formatDuration(item.duration)
        })
      })
      const list = append ? this.data.list.concat(records) : records
      this.setData({ list: list, finished: records.length < 10, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  onKeyword(e) { this.setData({ keyword: e.detail.value }) },
  onSearch() { this.reload() },
  goDetail(e) { wx.navigateTo({ url: '/pages/videos/detail?id=' + e.currentTarget.dataset.id }) }
})
