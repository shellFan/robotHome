const { tutorialApi } = require('../../api/index')
const { formatDate, imageOf } = require('../../utils/format')
Page({
  data: { item: null, loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const item = await tutorialApi.detail(this.id)
      this.setData({
        item: Object.assign({}, item, {
          cover: imageOf(item.cover),
          dateText: formatDate(item.publishTime || item.createTime, true),
          contentHtml: item.content || item.html || ''
        }),
        loading: false
      })
      wx.setNavigationBarTitle({ title: item.title || '教程详情' })
    } catch (e) { this.setData({ loading: false }) }
  }
})
