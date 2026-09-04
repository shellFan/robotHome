const { inquiryApi } = require('../../api/index')
const { formatDate, unwrapList } = require('../../utils/format')
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
      const data = await inquiryApi.my({ page: this.data.page, size: 10 })
      const records = unwrapList(data).map(function (x) {
        return Object.assign({}, x, {
          dateText: formatDate(x.createTime, true),
          statusText: x.statusName || x.status || '已提交'
        })
      })
      const list = append ? this.data.list.concat(records) : records
      this.setData({ list: list, finished: records.length < 10, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  }
})
