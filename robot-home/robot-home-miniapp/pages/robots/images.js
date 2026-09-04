const { robotApi } = require('../../api/index')
const { imageOf } = require('../../utils/format')
Page({
  data: { list: [], loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const data = await robotApi.images(this.id)
      const list = (Array.isArray(data) ? data : (data && data.records) || []).map(function (img) {
        const url = imageOf(img.url || img.imageUrl || img)
        return { url: url, title: img.title || '' }
      })
      this.setData({ list: list, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  preview(e) {
    const urls = this.data.list.map(function (i) { return i.url })
    wx.previewImage({ current: e.currentTarget.dataset.url, urls: urls })
  }
})
