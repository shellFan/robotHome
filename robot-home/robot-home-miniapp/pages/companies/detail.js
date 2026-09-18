var companyApi = require('../../api/index').companyApi
var followApi = require('../../api/index').followApi
var formatPrice = require('../../utils/format').formatPrice
var imageOf = require('../../utils/format').imageOf
var formatCount = require('../../utils/format').formatCount

Page({
  data: {
    company: null,
    followCount: 0,
    followed: false,
    brands: [],
    hotRobots: [],
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
    // Phase9: Use aggregated companyPage API
    companyApi.companyPage(that.id).then(function (data) {
      if (!data) data = {}
      var company = data.company || {}
      var mapRobot = function (r) {
        return Object.assign({}, r, {
          cover: imageOf(r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice)
        })
      }
      that.setData({
        company: Object.assign({}, company, { logo: imageOf(company.logo) }),
        followCount: data.followCount || 0,
        followed: data.followed || false,
        brands: (data.brands || []).map(function (b) {
          return Object.assign({}, b, { logo: imageOf(b.logo) })
        }),
        hotRobots: (data.hotRobots || []).map(mapRobot),
        articles: (data.articles || []).map(function (a) {
          return Object.assign({}, a, { cover: imageOf(a.cover), viewText: formatCount(a.viewCount || 0) })
        }),
        posts: (data.posts || []).map(function (p) {
          return Object.assign({}, p, { viewText: formatCount(p.viewCount || 0) })
        }),
        loading: false
      })
      wx.setNavigationBarTitle({ title: company.name || '企业详情' })
      wx.stopPullDownRefresh()
    }).catch(function () {
      // Fallback to separate API calls
      companyApi.detail(that.id).then(function (company) {
        companyApi.robots(that.id, { page: 1, size: 50 }).then(function (robotsData) {
          var records = (robotsData && robotsData.records) || (robotsData && robotsData.list) || []
          if (!Array.isArray(records)) records = []
          that.setData({
            company: Object.assign({}, company, { logo: imageOf(company.logo) }),
            hotRobots: records.map(function (r) {
              return Object.assign({}, r, { cover: imageOf(r.cover || r.mainImage), priceText: formatPrice(r.guidePrice) })
            }),
            loading: false
          })
          wx.setNavigationBarTitle({ title: company.name || '企业详情' })
        })
      }).catch(function () { that.setData({ loading: false }) })
      wx.stopPullDownRefresh()
    })
  },
  toggleFollow: function () {
    var that = this
    followApi.toggle('company', that.id).then(function (res) {
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
  goBrand: function (e) {
    wx.navigateTo({ url: '/pages/brands/detail?id=' + e.currentTarget.dataset.id })
  },
  goArticle: function (e) {
    wx.navigateTo({ url: '/pages/articles/detail?id=' + e.currentTarget.dataset.id })
  },
  goPost: function (e) {
    wx.navigateTo({ url: '/pages/community/post-detail?id=' + e.currentTarget.dataset.id })
  }
})