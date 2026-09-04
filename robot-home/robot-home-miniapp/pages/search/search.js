const { searchApi } = require('../../api/index')
Page({
  data: { keyword: '', hot: [], history: [], suggest: [] },
  onShow() { this.loadMeta() },
  async loadMeta() {
    try {
      const hot = await searchApi.hot(10)
      this.setData({ hot: Array.isArray(hot) ? hot : (hot && hot.list) || [] })
    } catch (e) {}
    try {
      const history = await searchApi.history(10)
      this.setData({ history: Array.isArray(history) ? history : (history && history.list) || [] })
    } catch (e) {
      const local = wx.getStorageSync('rh_search_history') || []
      this.setData({ history: local })
    }
  },
  onInput(e) {
    const keyword = e.detail.value
    this.setData({ keyword: keyword })
    this.suggest(keyword)
  },
  async suggest(keyword) {
    if (!keyword) return this.setData({ suggest: [] })
    try {
      const list = await searchApi.suggest(keyword, 8)
      this.setData({ suggest: Array.isArray(list) ? list : [] })
    } catch (e) {}
  },
  doSearch(e) {
    const kw = (e && e.currentTarget && e.currentTarget.dataset.kw) || this.data.keyword
    const keyword = String(kw || '').trim()
    if (!keyword) return
    let local = wx.getStorageSync('rh_search_history') || []
    local = [keyword].concat(local.filter(function (x) { return x !== keyword })).slice(0, 10)
    wx.setStorageSync('rh_search_history', local)
    wx.navigateTo({ url: '/pages/search/result?keyword=' + encodeURIComponent(keyword) })
  },
  clearHistory() {
    wx.removeStorageSync('rh_search_history')
    searchApi.clearHistory().catch(function () {})
    this.setData({ history: [] })
  }
})
