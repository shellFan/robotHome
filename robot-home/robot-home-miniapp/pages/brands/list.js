const { brandApi } = require('../../api/index')
const { imageOf, unwrapList } = require('../../utils/format')
Page({
  data: { list: [], page: 1, finished: false, loading: false, keyword: '', letter: '' },
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
      const data = await brandApi.page({ page: this.data.page, size: 20, keyword: this.data.keyword || undefined, letter: this.data.letter || undefined })
      const records = unwrapList(data).map(function (b) { return Object.assign({}, b, { logo: imageOf(b.logo) }) })
      const total = data && data.total != null ? data.total : records.length
      const list = append ? this.data.list.concat(records) : records
      this.setData({ list: list, finished: list.length >= total || records.length < 20, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  onKeyword(e) { this.setData({ keyword: e.detail.value }) },
  onSearch() { this.reload() },
  goDetail(e) { wx.navigateTo({ url: '/pages/brands/detail?id=' + e.currentTarget.dataset.id }) }
})
