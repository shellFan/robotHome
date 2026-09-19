var brandApi = require('../../api/index').brandApi
var followApi = require('../../api/index').followApi
var formatPrice = require('../../utils/format').formatPrice
var imageOf = require('../../utils/format').imageOf
var formatCount = require('../../utils/format').formatCount

Page({
  data: {
    brand: null,
    followCount: 0,
    followed: false,
    hotRobots: [],
    newRobots: [],
    articles: [],
    posts: [],
    loading: true
  },
  onLoad: function (q) {
    this.id = q.id
    this.load()
  },
  onPullDownRefresh: function () {
    this.load()
  },
  load: function () {
    var that = this
    that.setData({ loading: true })
    // Phase9: Use aggregated brandPage API
    brandApi.brandPage(that.id).then(function (data) {
      if (!data) data = {}
      var brand = data.brand || {}
      var mapRobot = function (r) {
        return Object.assign({}, r, {
          cover: imageOf(r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice)
        })
      }
      var mapArticle = function (a) {
        return Object.assign({}, a, {
          cover: imageOf(a.cover),
          viewText: formatCount(a.viewCount || 0)
        })
      }
      that.setData({
        brand: Object.assign({}, brand, { logo: imageOf(brand.logo) }),
        followCount: data.followCount || 0,
        followed: data.followed || false,
        hotRobots: (data.hotRobots || []).map(mapRobot),
        newRobots: (data.newRobots || []).map(mapRobot),
        articles: (data.articles || []).map(mapArticle),
        posts: (data.posts || []).map(function (p) {
          return Object.assign({}, p, { viewText: formatCount(p.viewCount || 0) })
        }),
        loading: false
      })
      wx.setNavigationBarTitle({ title: brand.name || '品牌详情' })
      wx.stopPullDownRefresh()
    }).catch(function () {
      // Fallback to separate API calls
      brandApi.detail(that.id).then(function (brand) {
        brandApi.robots(that.id, { page: 1, size: 50 }).then(function (robotsData) {
          var records = (robotsData && robotsData.records) || (robotsData && robotsData.list) || []
          if (!Array.isArray(records)) records = []
          that.setData({
            brand: Object.assign({}, brand, { logo: imageOf(brand.logo) }),
            hotRobots: records.map(function (r) {
              return Object.assign({}, r, { cover: imageOf(r.cover || r.mainImage), priceText: formatPrice(r.guidePrice) })
            }),
            loading: false
          })
          wx.setNavigationBarTitle({ title: brand.name || '品牌详情' })
        })
      }).catch(function () { that.setData({ loading: false }) })
      wx.stopPullDownRefresh()
    })
  },
  toggleFollow: function () {
    var that = this
    followApi.toggle('brand', that.id).then(function (res) {
      var followed = res && res.followed
      that.setData({
        followed: followed !== undefined ? followed : !that.data.followed,
        followCount: that.data.followCount + (followed ? 1 : -1)
      })
    }).catch(function () {})
  },
  goRobot: function (e) {
    wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id })
  },
  goArticle: function (e) {
    wx.navigateTo({ url: '/pages/articles/detail?id=' + e.currentTarget.dataset.id })
  },
  goPost: function (e) {
    wx.navigateTo({ url: '/pages/community/post-detail?id=' + e.currentTarget.dataset.id })
  }
})