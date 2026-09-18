var discoveryApi = require('../../api/index').discoveryApi
var rankingApi = require('../../api/index').rankingApi
var formatPrice = require('../../utils/format').formatPrice
var imageOf = require('../../utils/format').imageOf
var formatCount = require('../../utils/format').formatCount

Page({
  data: {
    loading: true,
    hotRobots: [],
    trendingRobots: [],
    newRobots: [],
    topRatedRobots: [],
    mostFavoritedRobots: [],
    mostDiscussedRobots: [],
    hotBrands: [],
    rankingCards: [],
    hotPosts: [],
    hotQuestions: []
  },
  onLoad: function () {
    this.load()
  },
  onPullDownRefresh: function () {
    this.load()
  },
  load: function () {
    var that = this
    that.setData({ loading: true })
    discoveryApi.home('miniapp').then(function (data) {
      if (!data) data = {}
      // Process robot lists
      var mapRobot = function (r) {
        return Object.assign({}, r, {
          cover: imageOf(r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice)
        })
      }
      var mapRankItem = function (item) {
        return Object.assign({}, item, {
          cover: imageOf(item.coverImage),
          priceText: formatPrice(item.guidePrice),
          rankChangeText: item.rankChange > 0 ? '+' + item.rankChange : (item.rankChange < 0 ? '' + item.rankChange : 'NEW')
        })
      }
      var mapBrand = function (b) {
        return Object.assign({}, b, { logo: imageOf(b.logo) })
      }
      var mapPost = function (p) {
        return Object.assign({}, p, { viewText: formatCount(p.viewCount || 0) })
      }
      that.setData({
        hotRobots: (data.hotRobots || []).map(mapRobot),
        trendingRobots: (data.trendingRobots || []).map(mapRobot),
        newRobots: (data.newRobots || []).map(mapRobot),
        topRatedRobots: (data.topRatedRobots || []).map(mapRobot),
        mostFavoritedRobots: (data.mostFavoritedRobots || []).map(mapRobot),
        mostDiscussedRobots: (data.mostDiscussedRobots || []).map(mapRobot),
        hotBrands: (data.hotBrands || []).map(mapBrand),
        rankingCards: (data.rankingCards || []).map(function (card) {
          return Object.assign({}, card, {
            items: (card.items || []).map(mapRankItem)
          })
        }),
        hotPosts: (data.hotPosts || []).map(mapPost),
        hotQuestions: (data.hotQuestions || []).map(function (q) {
          return Object.assign({}, q, { answerText: formatCount(q.answerCount || 0) })
        }),
        loading: false
      })
      wx.stopPullDownRefresh()
    }).catch(function () {
      that.setData({ loading: false })
      wx.stopPullDownRefresh()
    })
  },
  goRobot: function (e) {
    wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id })
  },
  goBrand: function (e) {
    wx.navigateTo({ url: '/pages/brands/detail?id=' + e.currentTarget.dataset.id })
  },
  goRanking: function (e) {
    var type = e.currentTarget.dataset.type || 'hot'
    wx.navigateTo({ url: '/pages/rankings/rankings?type=' + type })
  },
  goPost: function (e) {
    wx.navigateTo({ url: '/pages/community/post-detail?id=' + e.currentTarget.dataset.id })
  },
  goQuestion: function (e) {
    wx.navigateTo({ url: '/pages/qa/detail?id=' + e.currentTarget.dataset.id })
  }
})