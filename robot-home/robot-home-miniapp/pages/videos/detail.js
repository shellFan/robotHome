const { videoApi, likeApi, favoriteApi } = require('../../api/index')
const { formatCount, formatDuration, imageOf } = require('../../utils/format')
const { ensureLogin } = require('../../utils/request')
const { resolveVideoSource } = require('../../utils/video-url')

Page({
  data: { video: null, liked: false, favorited: false, loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const video = await videoApi.detail(this.id)
      const source = resolveVideoSource(video)
      this.setData({
        video: Object.assign({}, video, {
          cover: imageOf(video.cover),
          durationText: formatDuration(video.duration),
          viewText: formatCount(video.viewCount),
          playbackKind: source.kind,
          sourceUrl: source.url,
          playUrl: source.kind === 'media' ? source.url : '',
          externalUrl: source.kind === 'external' ? source.url : '',
          showPlayer: source.kind === 'media',
          showExternalLink: source.kind === 'external',
          showCopyOnFailure: false,
          showUnavailable: source.kind === 'none'
        }),
        loading: false
      })
      wx.setNavigationBarTitle({ title: video.title || '视频详情' })
    } catch (e) { this.setData({ loading: false }) }
  },
  onVideoError() {
    const video = this.data.video
    if (!video || video.playbackKind !== 'media') return
    this.setData({
      video: Object.assign({}, video, {
        showPlayer: false,
        showCopyOnFailure: true
      })
    })
  },
  onCopyVideoUrl() {
    const video = this.data.video
    const url = video && (video.externalUrl || (video.showCopyOnFailure && video.sourceUrl))
    if (!url) return

    wx.setClipboardData({
      data: url,
      success() {
        wx.showToast({
          title: video.externalUrl ? '已复制，请前往来源平台播放' : '已复制，可尝试在浏览器打开',
          icon: 'none'
        })
      },
      fail() {
        wx.showToast({ title: '复制失败，请稍后重试', icon: 'none' })
      }
    })
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
