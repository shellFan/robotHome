const { userApi } = require('../../api/index')
const { getToken, setToken } = require('../../utils/request')
const { imageOf } = require('../../utils/format')
const { userKey } = require('../../utils/config')

Page({
  data: {
    isLogin: false,
    user: null,
    stats: null
  },
  onShow() { this.refresh() },
  async refresh() {
    const token = getToken()
    if (!token) {
      this.setData({ isLogin: false, user: null, stats: null })
      return
    }
    this.setData({ isLogin: true })
    try {
      const user = await userApi.me()
      wx.setStorageSync(userKey, user)
      getApp().globalData.userInfo = user
      this.setData({
        user: Object.assign({}, user, { avatar: imageOf(user.avatar) })
      })
    } catch (e) {
      setToken('')
      this.setData({ isLogin: false, user: null })
      return
    }
    try {
      const stats = await userApi.stats()
      this.setData({ stats: stats })
    } catch (e) {}
  },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }) },
  goFavorites() { this.needLogin('/pages/mine/favorites') },
  goHistory() { this.needLogin('/pages/mine/history') },
  goPosts() { this.needLogin('/pages/mine/posts') },
  goInquiries() { this.needLogin('/pages/mine/inquiries') },
  goSettings() { wx.navigateTo({ url: '/pages/mine/settings' }) },
  goFeedback() { wx.navigateTo({ url: '/pages/feedback/feedback' }) },
  needLogin(url) {
    if (!getToken()) return this.goLogin()
    wx.navigateTo({ url: url })
  }
})
