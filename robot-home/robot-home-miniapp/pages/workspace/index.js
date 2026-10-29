const { enterpriseApi } = require('../../api/index')
const { imageOf } = require('../../utils/format')

Page({
  data: {
    loading: true,
    hasCompany: false,
    company: null,
    myRole: null,
    isVerified: false,
    claims: [],
    changes: [],
    commercial: null
  },
  onShow() { this.refresh() },
  async refresh() {
    this.setData({ loading: true })
    try {
      const data = await enterpriseApi.myCompany()
      if (data && data.company) {
        const company = Object.assign({}, data.company, {
          logo: imageOf(data.company.logo)
        })
        this.setData({
          hasCompany: true,
          company: company,
          myRole: data.myRole,
          isVerified: data.isVerified,
          loading: false
        })
        this.loadClaims()
        this.loadChanges()
        this.loadCommercial()
      } else {
        this.setData({
          hasCompany: false,
          company: null,
          loading: false
        })
        this.loadClaims()
      }
    } catch (e) {
      this.setData({ loading: false, hasCompany: false })
    }
  },
  async loadCommercial() {
    if (!this.data.company) return
    try {
      const { commercialApi } = require('../../api/index')
      const overview = await commercialApi.commercialOverview(this.data.company.id)
      const subscription = overview && overview.subscription ? Object.assign({}, overview.subscription, {
        endTimeText: this.formatDate(overview.subscription.endTime)
      }) : null
      this.setData({ commercial: Object.assign({}, overview, { subscription }) })
    } catch (e) { /* 商业概览加载失败不阻塞 */ }
  },
  async loadClaims() {
    try {
      const res = await enterpriseApi.myClaims({ pageNum: 1, pageSize: 5 })
      this.setData({ claims: res.list || [] })
    } catch (e) {}
  },
  async loadChanges() {
    try {
      const res = await enterpriseApi.myProfileChanges({ pageNum: 1, pageSize: 5 })
      this.setData({ changes: res.list || [] })
    } catch (e) {}
  },
  goClaimList() {
    wx.navigateTo({ url: '/pages/workspace/claims' })
  },
  goCompanyDetail() {
    if (this.data.company) {
      wx.navigateTo({ url: '/pages/companies/detail?id=' + this.data.company.id })
    }
  },
  goProcurement() {
    wx.navigateTo({ url: '/pages/procurement/index' })
  },
  goPlans() {
    wx.navigateTo({ url: '/pages/plans/plans' })
  },
  goOrders() {
    wx.navigateTo({ url: '/pages/orders/orders' })
  },
  goSubscription() {
    wx.navigateTo({ url: '/pages/subscription/subscription' })
  },
  statusText(status) {
    var map = { PENDING: '审核中', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }
    return map[status] || status
  },
  formatDate(val) {
    if (!val) return '-'
    return String(val).replace('T', ' ').substring(0, 10)
  }
})
