const { commercialApi, enterpriseApi } = require('../../api/index')
const { getToken } = require('../../utils/request')

Page({
  data: {
    loading: true,
    error: '',
    companyId: null,
    subscription: null,
    entitlements: [],
    isVerified: false,
    companyName: ''
  },
  onShow() { this.refresh() },
  async refresh() {
    if (!getToken()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const ws = await enterpriseApi.myCompany()
      if (!ws || !ws.company) {
        this.setData({ loading: false, error: '请先认领企业' })
        return
      }
      this.setData({
        companyId: ws.company.id,
        isVerified: !!ws.isVerified,
        companyName: ws.company.name
      })
      const [sub, ents] = await Promise.all([
        commercialApi.mySubscription(ws.company.id),
        commercialApi.myEntitlements(ws.company.id)
      ])
      this.setData({
        subscription: sub || null,
        entitlements: ents || []
      })
    } catch (e) {
      this.setData({ error: '订阅信息加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  entLabel(code) {
    var labels = { LEAD_VIEW: '商机查看', LEAD_RESPONSE: '商机响应', PRODUCT_MANAGEMENT: '产品管理', PRODUCT_FEATURED: '精选推荐位', AD_CAMPAIGN: '广告投放', ANALYTICS_ADVANCED: '高级数据分析', COMPANY_PROFILE_ENHANCED: '企业主页增强' }
    return labels[code] || code
  },
  statusText(status) {
    var map = { FREE: '免费版', ACTIVE: '使用中', EXPIRING: '即将到期', EXPIRED: '已过期', SUSPENDED: '已暂停', CANCELLED: '已取消' }
    return map[status] || status
  },
  formatDate(val) {
    if (!val) return '-'
    return String(val).replace('T', ' ').substring(0, 10)
  },
  async cancelRenew() {
    if (!this.data.companyId) return
    var that = this
    wx.showModal({
      title: '关闭自动续费',
      content: '关闭后到期将不再自动续费。不会解除外部支付代扣协议（如已签约）。',
      success: async function (res) {
        if (!res.confirm) return
        try {
          await commercialApi.cancelAutoRenew(that.data.companyId)
          wx.showToast({ title: '已关闭自动续费', icon: 'success' })
          await that.refresh()
        } catch (e) {
          wx.showToast({ title: e.message || '操作失败', icon: 'none' })
        }
      }
    })
  },
  goPlans() { wx.navigateTo({ url: '/pages/plans/plans' }) },
  goOrders() { wx.navigateTo({ url: '/pages/orders/orders' }) }
})