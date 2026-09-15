const { procurementApi } = require('../../api/index')
const { fromNow, unwrapList, formatDate } = require('../../utils/format')

Page({
  data: {
    category: '',
    usageScene: '',
    categories: ['工业制造', '物流仓储', '医疗健康', '教育培训', '服务行业', '其他'],
    scenes: ['INDUSTRIAL', 'LOGISTICS', 'MEDICAL', 'EDUCATION', 'SERVICE', 'OTHER'],
    inquiries: [],
    page: 1,
    finished: false,
    loading: false
  },
  onLoad() { this.reload() },
  onReachBottom() { this.more() },
  onPullDownRefresh() {
    this.reload().finally(function () { wx.stopPullDownRefresh() })
  },
  onCategoryChange(e) {
    this.setData({ category: this.data.categories[e.detail.value] })
    this.reload()
  },
  onSceneChange(e) {
    this.setData({ usageScene: this.data.scenes[e.detail.value] })
    this.reload()
  },
  async reload() {
    this.setData({ page: 1, inquiries: [], finished: false })
    await this.fetch()
  },
  async more() {
    if (this.data.loading || this.data.finished) return
    this.setData({ page: this.data.page + 1 })
    await this.fetch(true)
  },
  async fetch(append) {
    this.setData({ loading: true })
    try {
      const data = await procurementApi.hallList({
        category: this.data.category || undefined,
        usageScene: this.data.usageScene || undefined,
        pageNum: this.data.page,
        pageSize: 10
      })
      const records = unwrapList(data).map(function (item) {
        return Object.assign({}, item, {
          timeText: fromNow(item.createTime),
          dateText: formatDate(item.createTime),
          statusText: item.statusName || '待处理',
          quantityText: item.quantity ? item.quantity + '台' : '面议',
          budgetText: item.budget || '预算面议'
        })
      })
      const list = append ? this.data.inquiries.concat(records) : records
      this.setData({ inquiries: list, finished: records.length < 10, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  goDetail(e) {
    wx.navigateTo({ url: '/pages/procurement/detail?id=' + e.currentTarget.dataset.id })
  }
})