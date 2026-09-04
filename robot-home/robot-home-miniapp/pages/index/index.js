const { homeApi } = require('../../api/index')
const { formatPrice, formatCount, formatDate, imageOf } = require('../../utils/format')

Page({
  data: {
    loading: true,
    banners: [],
    quickNav: [],
    hotRobots: [],
    newRobots: [],
    rankings: [],
    hotBrands: [],
    articles: [],
    videos: []
  },
  onLoad() { this.loadData() },
  onPullDownRefresh() {
    this.loadData().finally(function () { wx.stopPullDownRefresh() })
  },
  async loadData() {
    this.setData({ loading: true })
    try {
      const data = await homeApi.index('miniapp')
      const mapRobot = function (r) {
        return Object.assign({}, r, {
          cover: imageOf(r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice)
        })
      }
      const rankings = (data.rankings || []).map(function (block) {
        return {
          code: block.code,
          name: block.name,
          robots: (block.robots || []).slice(0, 5).map(function (r, i) {
            return { id: r.id, name: r.name, priceText: formatPrice(r.guidePrice), rank: i + 1 }
          })
        }
      })
      this.setData({
        banners: (data.banners || []).map(function (b) { return Object.assign({}, b, { image: imageOf(b.image) }) }),
        quickNav: data.quickNav || data.categories || [],
        hotRobots: (data.hotRobots || []).slice(0, 6).map(mapRobot),
        newRobots: (data.newRobots || []).slice(0, 6).map(mapRobot),
        rankings: rankings,
        hotBrands: (data.hotBrands || []).slice(0, 8).map(function (b) { return Object.assign({}, b, { logo: imageOf(b.logo) }) }),
        articles: (data.articles || []).slice(0, 5).map(function (a) {
          return Object.assign({}, a, {
            cover: imageOf(a.cover),
            dateText: formatDate(a.publishTime),
            viewText: formatCount(a.viewCount)
          })
        }),
        videos: (data.videos || []).slice(0, 4).map(function (v) { return Object.assign({}, v, { cover: imageOf(v.cover) }) }),
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  onSearchTap() { wx.navigateTo({ url: '/pages/search/search' }) },
  onBannerTap(e) {
    const item = e.currentTarget.dataset.item
    if (item && item.linkType === 'robot' && item.linkId) {
      wx.navigateTo({ url: '/pages/robots/detail?id=' + item.linkId })
    }
  },
  goRobots(e) {
    const id = e.currentTarget.dataset.id
    if (id) wx.setStorageSync('rh_robot_filter', { categoryId: id })
    wx.switchTab({ url: '/pages/robots/list' })
  },
  goRobot(e) { wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id }) },
  goRankings(e) {
    const type = e.currentTarget.dataset.type || ''
    wx.navigateTo({ url: '/pages/rankings/rankings' + (type ? '?type=' + type : '') })
  },
  goBrand(e) { wx.navigateTo({ url: '/pages/brands/detail?id=' + e.currentTarget.dataset.id }) },
  goBrands() { wx.navigateTo({ url: '/pages/brands/list' }) },
  goArticle(e) { wx.navigateTo({ url: '/pages/articles/detail?id=' + e.currentTarget.dataset.id }) },
  goArticles() { wx.navigateTo({ url: '/pages/articles/list' }) },
  goVideo(e) { wx.navigateTo({ url: '/pages/videos/detail?id=' + e.currentTarget.dataset.id }) },
  goVideos() { wx.navigateTo({ url: '/pages/videos/list' }) },
  goCompare() { wx.navigateTo({ url: '/pages/compare/compare' }) }
})
