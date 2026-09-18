const { followApi } = require('../../api/index')
const { imageOf } = require('../../utils/format')

Page({
  data: {
    tabs: [
      { code: '', name: '全部' },
      { code: 'BRAND', name: '品牌' },
      { code: 'COMPANY', name: '企业' },
      { code: 'ROBOT', name: '机器人' },
      { code: 'USER', name: '用户' }
    ],
    tab: '',
    list: [],
    page: 1,
    finished: false,
    loading: false
  },
  onLoad() { this.reload() },
  onReachBottom() { this.more() },
  switchTab(e) {
    var tab = e.currentTarget.dataset.code
    if (tab === this.data.tab) return
    this.setData({ tab: tab })
    this.reload()
  },
  async reload() {
    this.setData({ page: 1, list: [], finished: false })
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
      var params = { pageNum: this.data.page, pageSize: 20 }
      if (this.data.tab) params.followType = this.data.tab
      var data = await followApi.list(params)
      var records = (data && data.records) || (data && data.list) || []
      if (!Array.isArray(records)) records = []
      records = records.map(function (item) {
        return Object.assign({}, item, {
          cover: imageOf(item.cover || item.avatar || item.logo),
          typeName: item.followType === 'BRAND' ? '品牌' : item.followType === 'COMPANY' ? '企业' : item.followType === 'ROBOT' ? '机器人' : item.followType === 'USER' ? '用户' : ''
        })
      })
      var list = append ? this.data.list.concat(records) : records
      this.setData({ list: list, finished: records.length < 20, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  async unfollow(e) {
    var item = e.currentTarget.dataset.item
    if (!item) return
    try {
      await followApi.remove(item.followType, item.followId)
      this.reload()
    } catch (e) { /* ignore */ }
  },
  goDetail(e) {
    var item = e.currentTarget.dataset.item
    if (!item) return
    if (item.followType === 'ROBOT') {
      wx.navigateTo({ url: '/pages/robots/detail?id=' + item.followId })
    } else if (item.followType === 'BRAND') {
      wx.navigateTo({ url: '/pages/brands/detail?id=' + item.followId })
    } else if (item.followType === 'COMPANY') {
      wx.navigateTo({ url: '/pages/companies/detail?id=' + item.followId })
    }
  }
})