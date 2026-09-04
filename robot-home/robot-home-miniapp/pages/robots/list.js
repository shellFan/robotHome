const { robotApi } = require('../../api/index')
const { formatPrice, imageOf, unwrapList } = require('../../utils/format')

Page({
  data: {
    list: [], page: 1, size: 10, total: 0, loading: false, finished: false,
    keyword: '', categoryId: '', brandId: '', sort: '',
    filters: null, showFilter: false
  },
  onLoad() {
    const cached = wx.getStorageSync('rh_robot_filter')
    if (cached) {
      wx.removeStorageSync('rh_robot_filter')
      this.setData({
        categoryId: cached.categoryId || '',
        brandId: cached.brandId || '',
        sort: cached.sort || ''
      })
    }
    this.loadFilters()
    this.reload()
  },
  onShow() {
    const cached = wx.getStorageSync('rh_robot_filter')
    if (cached) {
      wx.removeStorageSync('rh_robot_filter')
      this.setData({
        categoryId: cached.categoryId || '',
        brandId: cached.brandId || '',
        sort: cached.sort || '',
        page: 1, list: [], finished: false
      })
      this.reload()
    }
  },
  onReachBottom() { this.loadMore() },
  onPullDownRefresh() {
    this.reload().finally(function () { wx.stopPullDownRefresh() })
  },
  async loadFilters() {
    try {
      const filters = await robotApi.filters()
      this.setData({ filters: filters })
    } catch (e) {}
  },
  async reload() {
    this.setData({ page: 1, list: [], finished: false })
    await this.fetch()
  },
  async loadMore() {
    if (this.data.loading || this.data.finished) return
    this.setData({ page: this.data.page + 1 })
    await this.fetch(true)
  },
  async fetch(append) {
    this.setData({ loading: true })
    try {
      const data = await robotApi.page({
        page: this.data.page,
        size: this.data.size,
        keyword: this.data.keyword || undefined,
        categoryId: this.data.categoryId || undefined,
        brandId: this.data.brandId || undefined,
        sort: this.data.sort || undefined
      })
      const records = unwrapList(data).map(function (r) {
        return Object.assign({}, r, {
          cover: imageOf(r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice)
        })
      })
      const total = data && data.total != null ? data.total : records.length
      const list = append ? this.data.list.concat(records) : records
      this.setData({
        list: list,
        total: total,
        finished: list.length >= total || records.length < this.data.size,
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  onKeyword(e) { this.setData({ keyword: e.detail.value }) },
  onSearch() { this.reload() },
  toggleFilter() { this.setData({ showFilter: !this.data.showFilter }) },
  setSort(e) {
    this.setData({ sort: e.currentTarget.dataset.sort || '' })
    this.reload()
  },
  setCategory(e) {
    this.setData({ categoryId: e.currentTarget.dataset.id || '' })
    this.reload()
  },
  setBrand(e) {
    this.setData({ brandId: e.currentTarget.dataset.id || '' })
    this.reload()
  },
  clearFilter() {
    this.setData({ categoryId: '', brandId: '', sort: '', keyword: '' })
    this.reload()
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id }) },
  goCompare() { wx.navigateTo({ url: '/pages/compare/compare' }) }
})
