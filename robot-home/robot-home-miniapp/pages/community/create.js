const { communityApi } = require('../../api/index')
const { ensureLogin } = require('../../utils/request')
Page({
  data: {
    circles: [], circleId: '', circleName: '选择圈子', title: '', content: '', submitting: false
  },
  onLoad() {
    if (!ensureLogin()) return
    this.loadCircles()
  },
  async loadCircles() {
    try {
      const circles = await communityApi.circles()
      const list = Array.isArray(circles) ? circles : []
      this.setData({
        circles: list,
        circleNames: list.map(function (c) { return c.name })
      })
    } catch (e) {}
  },
  onCircleChange(e) {
    const idx = Number(e.detail.value)
    const c = this.data.circles[idx]
    if (c) this.setData({ circleId: c.id, circleName: c.name })
  },
  onTitle(e) { this.setData({ title: e.detail.value }) },
  onContent(e) { this.setData({ content: e.detail.value }) },
  async submit() {
    if (!ensureLogin()) return
    const title = (this.data.title || '').trim()
    const content = (this.data.content || '').trim()
    if (!content) return wx.showToast({ title: '请填写内容', icon: 'none' })
    if (this.data.submitting) return
    this.setData({ submitting: true })
    try {
      await communityApi.create({
        title: title,
        content: content,
        circleId: this.data.circleId || undefined
      })
      wx.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 500)
    } catch (e) {
      this.setData({ submitting: false })
    }
  }
})
