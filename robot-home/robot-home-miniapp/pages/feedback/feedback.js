var feedbackApi = require('../../api/index').feedbackApi
var behaviorApi = require('../../api/index').behaviorApi

Page({
  data: {
    types: [
      { code: 'bug', name: 'Bug反馈' },
      { code: 'feature', name: '功能建议' },
      { code: 'improvement', name: '体验改进' },
      { code: 'other', name: '其他' }
    ],
    feedbackType: 'bug',
    content: '',
    contact: '',
    submitting: false,
    done: false
  },
  switchType: function (e) {
    this.setData({ feedbackType: e.currentTarget.dataset.code })
  },
  onContentInput: function (e) {
    this.setData({ content: e.detail.value })
  },
  onContactInput: function (e) {
    this.setData({ contact: e.detail.value })
  },
  async submit() {
    var content = this.data.content.trim()
    if (!content) {
      wx.showToast({ title: '请输入反馈内容', icon: 'none' })
      return
    }
    if (content.length < 5) {
      wx.showToast({ title: '内容至少5个字', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      var pages = getCurrentPages()
      var pageUrl = pages.length ? '/' + pages[pages.length - 1].route : ''
      await feedbackApi.submit({
        feedbackType: this.data.feedbackType,
        content: content,
        contact: this.data.contact.trim(),
        pageUrl: pageUrl
      })
      behaviorApi.track('submit_feedback', 'feedback', 0, { feedbackType: this.data.feedbackType })
      this.setData({ done: true })
      wx.showToast({ title: '提交成功', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: '提交失败，请重试', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
  goBack: function () {
    wx.navigateBack({ delta: 1 })
  },
  submitAgain: function () {
    this.setData({ done: false, content: '', contact: '', feedbackType: 'bug' })
  }
})