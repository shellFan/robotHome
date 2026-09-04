const { videoApi, likeApi, favoriteApi } = require('../../api/index')
const { formatCount, formatDuration, imageOf } = require('../../utils/format')
const { ensureLogin } = require('../../utils/request')
Page({
  data: { video: null, liked: false, favorited: false, loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const video = await videoApi.detail(this.id)
      this.setData({
        video: Object.assign({}, video, {
          cover: imageOf(video.cover),
          durationText: formatDuration(video.duration),
          viewText: formatCount(video.viewCount),
          playUrl: video.playUrl || video.url || video.videoUrl || ''
        }),
        loading: false
      })
      wx.setNavigationBarTitle({ title: video.title || '视频详情' })
    } catch (e) { this.setData({ loading: false }) }
  },
  async onLike() {
    if (!ensureLogin()) return
    try { await likeApi.toggle('video', this.id); this.setData({ liked: !this.data.liked }) } catch (e) {}
  },
  async onFavorite() {
    if (!ensureLogin()) return
    try { await favoriteApi.toggle('video', this.id); this.setData({ favorited: !this.data.favorited }) } catch (e) {}
  }
})
