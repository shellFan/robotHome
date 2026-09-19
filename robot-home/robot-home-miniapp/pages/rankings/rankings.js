var rankingApi = require('../../api/index').rankingApi
var formatPrice = require('../../utils/format').formatPrice
var imageOf = require('../../utils/format').imageOf

Page({
  data: {
    types: [],
    type: 'hot',
    timeRange: 'all',
    list: [],
    loading: true,
    useSnapshot: true,
    timeOptions: [
      { code: 'all', name: '全部' },
      { code: 'week', name: '近7天' },
      { code: 'month', name: '近30天' }
    ]
  },
  onLoad: function (q) {
    this.setData({ type: q.type || 'hot' })
    this.loadTypes()
    this.load()
  },
  onPullDownRefresh: function () {
    this.load()
  },
  loadTypes: function () {
    var that = this
    rankingApi.types().then(function (types) {
      var arr = Array.isArray(types) ? types : (types && types.list) || []
      that.setData({ types: arr.length ? arr : [
        { code: 'hot', name: '热门榜' },
        { code: 'new', name: '新品榜' },
        { code: 'sales', name: '热销榜' }
      ]})
    }).catch(function () {
      that.setData({ types: [
        { code: 'hot', name: '热门榜' },
        { code: 'new', name: '新品榜' }
      ]})
    })
  },
  load: function () {
    var that = this
    that.setData({ loading: true })
    // Phase9: Use snapshot API for rank changes
    rankingApi.snapshot(that.data.type, 50).then(function (data) {
      var list = []
      if (Array.isArray(data)) list = data
      else if (data && data.list) list = data.list
      list = list.map(function (r, i) {
        var changeText = 'NEW'
        var changeClass = 'rank-new'
        if (r.rankChange > 0) { changeText = '+' + r.rankChange; changeClass = 'rank-up' }
        else if (r.rankChange < 0) { changeText = '' + r.rankChange; changeClass = 'rank-down' }
        return Object.assign({}, r, {
          rank: r.rankNo || (i + 1),
          cover: imageOf(r.coverImage || r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice),
          changeText: changeText,
          changeClass: changeClass
        })
      })
      that.setData({ list: list, loading: false })
      wx.stopPullDownRefresh()
    }).catch(function () {
      // Fallback to old API if snapshot not available
      rankingApi.rank(that.data.type, 50, that.data.timeRange).then(function (data) {
        var list = []
        if (Array.isArray(data)) list = data
        else if (data && data.robots) list = data.robots
        else if (data && data.list) list = data.list
        list = list.map(function (r, i) {
          return Object.assign({}, r, {
            rank: i + 1,
            cover: imageOf(r.cover || r.mainImage),
            priceText: formatPrice(r.guidePrice),
            changeText: '',
            changeClass: ''
          })
        })
        that.setData({ list: list, loading: false })
        wx.stopPullDownRefresh()
      }).catch(function () {
        that.setData({ loading: false })
        wx.stopPullDownRefresh()
      })
    })
  },
  switchType: function (e) {
    this.setData({ type: e.currentTarget.dataset.code })
    this.load()
  },
  switchTime: function (e) {
    this.setData({ timeRange: e.currentTarget.dataset.code })
    this.load()
  },
  goDetail: function (e) {
    wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id })
  }
})