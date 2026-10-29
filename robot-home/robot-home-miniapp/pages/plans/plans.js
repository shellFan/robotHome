const { commercialApi, enterpriseApi } = require('../../api/index')
const { getToken } = require('../../utils/request')

Page({
  data: {
    loading: true,
    error: '',
    plans: [],
    companyId: null,
    currentPlanCode: '',
    purchasing: false
  },
  onShow() { this.refresh() },
  async refresh() {
    this.setData({ loading: true, error: '' })
    try {
      const plans = await commercialApi.plans()
      const normalizedPlans = (plans || []).map((plan) => Object.assign({}, plan, {
        priceText: this.formatPrice(plan.priceCents, plan.displayPrice)
      }))
      this.setData({ plans: normalizedPlans })
      if (getToken()) {
        try {
          const ws = await enterpriseApi.myCompany()
          if (ws && ws.company) {
            this.setData({ companyId: ws.company.id })
            const sub = await commercialApi.mySubscription(ws.company.id)
            if (sub) this.setData({ currentPlanCode: sub.planName || '' })
          }
        } catch (e) { /* 未认领企业 */ }
      }
    } catch (e) {
      this.setData({ error: '套餐加载失败，请下拉重试' })
    } finally {
      this.setData({ loading: false })
    }
  },
  onPullDownRefresh() {
    this.refresh().then(function () { wx.stopPullDownRefresh() })
  },
  entLabels(config) {
    if (!config) return []
    try {
      var arr = typeof config === 'string' ? JSON.parse(config) : config
      var labels = { LEAD_VIEW: '商机查看', LEAD_RESPONSE: '商机响应', PRODUCT_MANAGEMENT: '产品管理', PRODUCT_FEATURED: '精选推荐位', AD_CAMPAIGN: '广告投放', ANALYTICS_ADVANCED: '高级数据分析', COMPANY_PROFILE_ENHANCED: '企业主页增强' }
      return (arr || []).map(function (e) { return labels[e.code] || e.code })
    } catch (e) { return [] }
  },
  formatPrice(cents, displayPrice) {
    if (cents === null || cents === undefined || cents === '') return displayPrice || '暂无报价'
    var amount = Number(cents)
    if (isNaN(amount)) return displayPrice || '暂无报价'
    return '¥' + (amount / 100).toFixed(2)
  },
  async buyPlan(e) {
    if (this.data.purchasing) return
    var planId = e.currentTarget.dataset.id
    var planName = e.currentTarget.dataset.name
    if (!getToken()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    if (!this.data.companyId) {
      wx.showModal({ title: '提示', content: '请先认领企业后再购买套餐', showCancel: false })
      return
    }
    var that = this
    wx.showModal({
      title: '确认购买',
      content: '确认为企业购买「' + planName + '」套餐？',
      success: async function (res) {
        if (!res.confirm) return
        that.setData({ purchasing: true })
        try {
          var idemKey = 'miniapp-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8)
          var order = await commercialApi.createOrder({
            companyId: that.data.companyId,
            planId: planId,
            idempotentKey: idemKey
          })
          wx.showToast({ title: '订单已创建', icon: 'success' })
          wx.navigateTo({ url: '/pages/orders/orders' })
        } catch (e) {
          wx.showToast({ title: e.message || '购买失败', icon: 'none' })
        } finally {
          that.setData({ purchasing: false })
        }
      }
    })
  },
  goOrders() { wx.navigateTo({ url: '/pages/orders/orders' }) },
  goSubscription() { wx.navigateTo({ url: '/pages/subscription/subscription' }) }
})
