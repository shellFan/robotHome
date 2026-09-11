const { robotApi, favoriteApi, behaviorApi } = require('../../api/index')
const { formatPrice, formatCount, imageOf, parseMainParams } = require('../../utils/format')
const { toggleCompare, getCompareIds } = require('../../utils/compare')
const { ensureLogin, getToken } = require('../../utils/request')

Page({
  data: {
    id: null, robot: null, params: [], favorited: false, inCompare: false, loading: true
  },
  onLoad(q) {
    this.setData({ id: q.id })
    this.load()
  },
  async load() {
    this.setData({ loading: true })
    try {
      const robot = await robotApi.detail(this.data.id)
      robotApi.view(this.data.id).catch(function () {})
      behaviorApi.track('view', 'robot', Number(this.data.id))
      const ids = getCompareIds()
      let favorited = false
      if (getToken()) {
        try {
          const c = await favoriteApi.check('robot', this.data.id)
          favorited = !!(c === true || (c && c.favorited) || (c && c.liked))
        } catch (e) {}
      }
      this.setData({
        robot: Object.assign({}, robot, {
          cover: imageOf(robot.cover || robot.mainImage),
          priceText: formatPrice(robot.guidePrice),
          viewText: formatCount(robot.viewCount)
        }),
        params: parseMainParams(robot.mainParams, 6),
        favorited: favorited,
        inCompare: ids.indexOf(Number(this.data.id)) >= 0,
        loading: false
      })
      wx.setNavigationBarTitle({ title: robot.name || '机器人详情' })
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  goParams() { wx.navigateTo({ url: '/pages/robots/params?id=' + this.data.id }) },
  goImages() { wx.navigateTo({ url: '/pages/robots/images?id=' + this.data.id }) },
  goVideos() { wx.navigateTo({ url: '/pages/robots/videos?id=' + this.data.id }) },
  goInquiry() {
    behaviorApi.track('inquiry', 'robot', Number(this.data.id))
    wx.navigateTo({ url: '/pages/inquiry/inquiry?robotId=' + this.data.id + '&robotName=' + encodeURIComponent((this.data.robot && this.data.robot.name) || '') })
  },
  onCompare() {
    const r = toggleCompare(this.data.id)
    if (!r.full) {
      this.setData({ inCompare: r.added || r.ids.indexOf(Number(this.data.id)) >= 0 })
      behaviorApi.track(r.added ? 'add_compare' : 'remove_compare', 'robot', Number(this.data.id))
      wx.showToast({ title: r.added ? '已加入对比' : '已移出对比', icon: 'none' })
    }
  },
  goComparePage() { wx.navigateTo({ url: '/pages/compare/compare' }) },
  async onFavorite() {
    if (!ensureLogin()) return
    try {
      await favoriteApi.toggle('robot', this.data.id)
      const next = !this.data.favorited
      this.setData({ favorited: next })
      behaviorApi.track(next ? 'favorite' : 'unfavorite', 'robot', Number(this.data.id))
      wx.showToast({ title: next ? '已收藏' : '已取消', icon: 'none' })
    } catch (e) {}
  },
  previewCover() {
    if (this.data.robot && this.data.robot.cover) {
      wx.previewImage({ urls: [this.data.robot.cover] })
    }
  }
})
