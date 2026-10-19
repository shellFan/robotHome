const { commercialApi, enterpriseApi } = require('../../api/index')
const { getToken } = require('../../utils/request')

Page({
  data: {
    loading: true,
    error: '',
    orders: [],
    companyId: null,
    statusFilter: '',
    operating: false,
    channels: []
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
      this.setData({ companyId: ws.company.id })
      await this.loadOrders()
      try {
        const ch = await commercialApi.paymentChannels()
        this.setData({ channels: ch || [] })
      } catch (e) {}
    } catch (e) {
      this.setData({ error: '加载失败，请重试' })
    } finally {
      this.setData({ loading: false })
    }
  },
  async loadOrders() {
    if (!this.data.companyId) return
    try {
      var params = { companyId: this.data.companyId, pageNum: 1, pageSize: 20 }
      if (this.data.statusFilter) params.status = this.data.statusFilter
      const res = await commercialApi.myOrders(params)
      this.setData({ orders: (res && res.list) || [] })
    } catch (e) {
      wx.showToast({ title: '订单加载失败', icon: 'none' })
    }
  },
  onPullDownRefresh() {
    this.loadOrders().then(function () { wx.stopPullDownRefresh() })
  },
  switchStatus(e) {
    this.setData({ statusFilter: e.currentTarget.dataset.status })
    this.loadOrders()
  },
  statusText(status) {
    var map = { PENDING_PAYMENT: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款', CLOSED: '已关闭' }
    return map[status] || status
  },
  async cancelOrder(e) {
    if (this.data.operating) return
    var orderId = e.currentTarget.dataset.id
    var that = this
    wx.showModal({
      title: '取消订单',
      content: '确定取消该待支付订单？',
      success: async function (res) {
        if (!res.confirm) return
        that.setData({ operating: true })
        try {
          await commercialApi.cancelOrder(orderId, that.data.companyId)
          wx.showToast({ title: '已取消', icon: 'success' })
          await that.loadOrders()
        } catch (e) {
          wx.showToast({ title: e.message || '取消失败', icon: 'none' })
        } finally {
          that.setData({ operating: false })
        }
      }
    })
  },
  async payOrder(e) {
    if (this.data.operating) return
    var orderId = e.currentTarget.dataset.id
    var that = this
    that.setData({ operating: true })
    try {
      var result = await commercialApi.payOrder(orderId, that.data.companyId, 'MOCK_SANDBOX')
      wx.showModal({
        title: '沙箱支付',
        content: '已发起沙箱支付，是否确认支付？（仅沙箱环境可用）',
        success: async function (res) {
          if (!res.confirm) return
          try {
            var confirmRes = await commercialApi.sandboxConfirm(orderId, that.data.companyId)
            if (confirmRes && confirmRes.success) {
              wx.showToast({ title: '支付成功', icon: 'success' })
            } else {
              wx.showToast({ title: '支付确认失败', icon: 'none' })
            }
            await that.loadOrders()
          } catch (e2) {
            wx.showToast({ title: e2.message || '支付确认失败', icon: 'none' })
          }
        }
      })
    } catch (e) {
      wx.showToast({ title: e.message || '支付发起失败', icon: 'none' })
    } finally {
      that.setData({ operating: false })
    }
  },
  formatDate(val) {
    if (!val) return ''
    return String(val).replace('T', ' ').substring(0, 16)
  },
  goPlans() { wx.navigateTo({ url: '/pages/plans/plans' }) }
})