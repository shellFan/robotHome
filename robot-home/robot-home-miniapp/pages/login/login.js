const { authApi, userApi } = require('../../api/index')
const { setToken } = require('../../utils/request')
const { userKey } = require('../../utils/config')

Page({
  data: {
    mode: 'password', // password | sms | wx
    username: '', password: '',
    phone: '', code: '',
    openid: 'dev_openid_',
    countdown: 0,
    submitting: false
  },
  onLoad() {
    this.setData({ openid: 'dev_openid_' + Date.now().toString(36) })
  },
  switchMode(e) { this.setData({ mode: e.currentTarget.dataset.mode }) },
  onUsername(e) { this.setData({ username: e.detail.value }) },
  onPassword(e) { this.setData({ password: e.detail.value }) },
  onPhone(e) { this.setData({ phone: e.detail.value }) },
  onCode(e) { this.setData({ code: e.detail.value }) },
  onOpenid(e) { this.setData({ openid: e.detail.value }) },
  async sendCode() {
    const phone = (this.data.phone || '').trim()
    if (!/^1\d{10}$/.test(phone)) return wx.showToast({ title: '请输入正确手机号', icon: 'none' })
    if (this.data.countdown > 0) return
    try {
      await authApi.sendSmsCode(phone, 'login')
      wx.showToast({ title: '验证码已发送', icon: 'none' })
      this.setData({ countdown: 60 })
      this._timer = setInterval(() => {
        const n = this.data.countdown - 1
        this.setData({ countdown: n })
        if (n <= 0) clearInterval(this._timer)
      }, 1000)
    } catch (e) {}
  },
  async afterLogin(res) {
    const token = (res && (res.token || res.accessToken)) || ''
    if (!token) {
      wx.showToast({ title: '登录返回异常', icon: 'none' })
      return
    }
    setToken(token)
    let user = res.user || res.profile || null
    try {
      if (!user) user = await userApi.me()
    } catch (e) {}
    if (user) {
      wx.setStorageSync(userKey, user)
      getApp().globalData.userInfo = user
    }
    wx.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(function () {
      const pages = getCurrentPages()
      if (pages.length > 1) wx.navigateBack()
      else wx.switchTab({ url: '/pages/mine/index' })
    }, 400)
  },
  async loginPassword() {
    const username = (this.data.username || '').trim()
    const password = this.data.password || ''
    if (!username || !password) return wx.showToast({ title: '请输入账号密码', icon: 'none' })
    this.setData({ submitting: true })
    try {
      const res = await authApi.login(username, password)
      await this.afterLogin(res)
    } catch (e) {}
    this.setData({ submitting: false })
  },
  async loginSms() {
    const phone = (this.data.phone || '').trim()
    const code = (this.data.code || '').trim()
    if (!phone || !code) return wx.showToast({ title: '请输入手机号和验证码', icon: 'none' })
    this.setData({ submitting: true })
    try {
      const res = await authApi.smsLogin(phone, code)
      await this.afterLogin(res)
    } catch (e) {}
    this.setData({ submitting: false })
  },
  async loginWx() {
    const openid = (this.data.openid || '').trim()
    if (!openid) return wx.showToast({ title: '请填写 openid', icon: 'none' })
    this.setData({ submitting: true })
    try {
      const res = await authApi.wxLogin(openid, '微信用户', '')
      await this.afterLogin(res)
    } catch (e) {}
    this.setData({ submitting: false })
  },
  onUnload() {
    if (this._timer) clearInterval(this._timer)
  }
})
