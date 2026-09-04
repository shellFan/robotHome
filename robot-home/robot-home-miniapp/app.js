App({
  globalData: {
    userInfo: null
  },
  onLaunch: function () {
    try {
      const user = wx.getStorageSync('rh_user')
      if (user) this.globalData.userInfo = user
    } catch (e) {}
  }
})
