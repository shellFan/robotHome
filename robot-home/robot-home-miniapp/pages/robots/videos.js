const { robotApi } = require('../../api/index')
const { imageOf, formatDuration } = require('../../utils/format')
Page({
  data: { list: [], loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const data = await robotApi.videos(this.id)
      const list = (Array.isArray(data) ? data : (data && data.records) || []).map(function (v) {
        return Object.assign({}, v, {
          cover: imageOf(v.cover),
          durationText: formatDuration(v.duration)
        })
      })
      this.setData({ list: list, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/videos/detail?id=' + e.currentTarget.dataset.id }) }
})
