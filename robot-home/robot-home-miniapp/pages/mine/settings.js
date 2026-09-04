const { authApi } = require('../../api/index')
const { getToken, setToken } = require('../../utils/request')
const { baseUrl, userKey } = require('../../utils/config')
Page({
  data: { baseUrl: baseUrl, isLogin: false },
  onShow() { this.setData({ isLogin: !!getToken() }) },
  copyUrl() {
    wx.setClipboardData({ data: this.data.baseUrl })
  },
  async logout() {
    try { await authApi.logout() } catch (e) {}
    setToken('')
    wx.removeStorageSync(userKey)
    getApp().globalData.userInfo = null
    wx.showToast({ title: '已退出', icon: 'none' })
    this.setData({ isLogin: false })
  },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }) }
})
