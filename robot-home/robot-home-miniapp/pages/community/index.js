const { communityApi, likeApi } = require('../../api/index')
const { fromNow, imageOf, unwrapList, formatCount } = require('../../utils/format')
const { ensureLogin } = require('../../utils/request')

Page({
  data: {
    circles: [], circleId: '', posts: [], page: 1, finished: false, loading: false
  },
  onLoad() {
    this.loadCircles()
    this.reload()
  },
  onShow() {},
  onReachBottom() { this.more() },
  onPullDownRefresh() {
    this.reload().finally(function () { wx.stopPullDownRefresh() })
  },
  async loadCircles() {
    try {
      const circles = await communityApi.circles()
      this.setData({ circles: Array.isArray(circles) ? circles : [] })
    } catch (e) {}
  },
  async reload() {
    this.setData({ page: 1, posts: [], finished: false })
    await this.fetch()
  },
  async more() {
    if (this.data.loading || this.data.finished) return
    this.setData({ page: this.data.page + 1 })
    await this.fetch(true)
  },
  async fetch(append) {
    this.setData({ loading: true })
    try {
      const data = await communityApi.posts({
        page: this.data.page,
        size: 10,
        circleId: this.data.circleId || undefined
      })
      const records = unwrapList(data).map(function (p) {
        return Object.assign({}, p, {
          avatar: imageOf(p.avatar || p.userAvatar),
          cover: imageOf(p.cover || (p.images && p.images[0])),
          timeText: fromNow(p.createTime),
          likeText: formatCount(p.likeCount),
          commentText: formatCount(p.commentCount)
        })
      })
      const list = append ? this.data.posts.concat(records) : records
      this.setData({ posts: list, finished: records.length < 10, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  switchCircle(e) {
    this.setData({ circleId: e.currentTarget.dataset.id || '' })
    this.reload()
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/community/post-detail?id=' + e.currentTarget.dataset.id }) },
  goCreate() {
    if (!ensureLogin()) return
    wx.navigateTo({ url: '/pages/community/create' })
  }
})
