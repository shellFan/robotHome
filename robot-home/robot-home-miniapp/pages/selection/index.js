const { selectionApi } = require('../../api/index')
const { imageOf, formatPrice, unwrapList } = require('../../utils/format')

Page({
  data: {
    category: '',
    budgetMin: '',
    budgetMax: '',
    usage: '',
    keyword: '',
    categories: [],
    usages: [],
    results: [],
    page: 1,
    finished: false,
    loading: false,
    searched: false,
    showFilters: false
  },
  onLoad() { this.loadFilters() },
  async loadFilters() {
    try {
      const data = await selectionApi.filters()
      this.setData({
        categories: data.categories || [],
        usages: data.usages || []
      })
    } catch (e) {}
  },
  onCategory(e) { this.setData({ category: e.detail.value }) },
  onBudgetMin(e) { this.setData({ budgetMin: e.detail.value }) },
  onBudgetMax(e) { this.setData({ budgetMax: e.detail.value }) },
  onUsage(e) { this.setData({ usage: e.detail.value }) },
  onKeyword(e) { this.setData({ keyword: e.detail.value }) },
  toggleFilters() { this.setData({ showFilters: !this.data.showFilters }) },
  resetFilters() {
    this.setData({ category: '', budgetMin: '', budgetMax: '', usage: '', keyword: '' })
  },
  async search() {
    this.setData({ page: 1, results: [], finished: false, searched: true })
    await this.fetch()
  },
  async more() {
    if (this.data.loading || this.data.finished) return
    this.setData({ page: this.data.page + 1 })
    await this.fetch(true)
  },
  onReachBottom() { this.more() },
  async fetch(append) {
    this.setData({ loading: true })
    try {
      const params = {
        category: this.data.category || undefined,
        budgetMin: this.data.budgetMin ? Number(this.data.budgetMin) : undefined,
        budgetMax: this.data.budgetMax ? Number(this.data.budgetMax) : undefined,
        usage: this.data.usage || undefined,
        pageNum: this.data.page,
        pageSize: 10
      }
      const data = await selectionApi.search(params)
      const records = unwrapList(data).map(function (r) {
        return Object.assign({}, r, {
          cover: imageOf(r.coverImage),
          priceText: formatPrice(r.guidePrice),
          scoreText: r.matchScore ? r.matchScore + '%' : '--'
        })
      })
      const list = append ? this.data.results.concat(records) : records
      this.setData({ results: list, finished: records.length < 10, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  goRobot(e) {
    wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id })
  },
  goInquiry(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: '/pages/inquiry/inquiry?robotId=' + item.robotId + '&robotName=' + encodeURIComponent(item.robotName)
    })
  }
})