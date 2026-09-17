const { qaApi, commentApi } = require('../../api/index')
const { fromNow, imageOf, formatCount } = require('../../utils/format')
const { ensureLogin } = require('../../utils/request')

Page({
  data: {
    question: null,
    answers: [],
    answerText: '',
    followed: false,
    loading: true,
    answerPage: 1,
    answerFinished: false,
    answerLoading: false
  },
  onLoad(q) { this.id = q.id; this.load() },
  onReachBottom() { this.moreAnswers() },
  async load() {
    try {
      const q = await qaApi.questionDetail(this.id)
      this.setData({
        question: Object.assign({}, q, {
          avatar: imageOf(q.authorAvatar),
          timeText: fromNow(q.createTime),
          answerText: formatCount(q.answerCount),
          followText: formatCount(q.followCount),
          viewText: formatCount(q.viewCount)
        }),
        followed: q.followed || false,
        loading: false
      })
      this.loadAnswers()
    } catch (e) { this.setData({ loading: false }) }
  },
  async loadAnswers() {
    this.setData({ answerPage: 1, answers: [], answerFinished: false })
    await this.fetchAnswers()
  },
  async moreAnswers() {
    if (this.data.answerLoading || this.data.answerFinished) return
    this.setData({ answerPage: this.data.answerPage + 1 })
    await this.fetchAnswers(true)
  },
  async fetchAnswers(append) {
    this.setData({ answerLoading: true })
    try {
      const data = await qaApi.answers(this.id, { pageNum: this.data.answerPage, pageSize: 20 })
      const records = (data && (data.records || data.list || data)) || []
      const answers = (Array.isArray(records) ? records : []).map(function (a) {
        return Object.assign({}, a, {
          avatar: imageOf(a.authorAvatar),
          timeText: fromNow(a.createTime),
          helpfulText: formatCount(a.helpfulCount)
        })
      })
      const list = append ? this.data.answers.concat(answers) : answers
      this.setData({ answers: list, answerFinished: answers.length < 20, answerLoading: false })
    } catch (e) { this.setData({ answerLoading: false }) }
  },
  onAnswerInput(e) { this.setData({ answerText: e.detail.value }) },
  async submitAnswer() {
    if (!ensureLogin()) return
    const content = (this.data.answerText || '').trim()
    if (!content) return wx.showToast({ title: '请输入回答', icon: 'none' })
    try {
      await qaApi.answer(this.id, { content: content })
      this.setData({ answerText: '' })
      wx.showToast({ title: '回答成功', icon: 'success' })
      this.loadAnswers()
    } catch (e) {}
  },
  async toggleFollow() {
    if (!ensureLogin()) return
    try {
      if (this.data.followed) {
        await qaApi.unfollow(this.id)
      } else {
        await qaApi.follow(this.id)
      }
      this.setData({ followed: !this.data.followed })
    } catch (e) {}
  },
  async helpful(e) {
    if (!ensureLogin()) return
    const id = e.currentTarget.dataset.id
    try {
      await qaApi.helpful(id)
      wx.showToast({ title: '感谢反馈', icon: 'success' })
      this.loadAnswers()
    } catch (e) {}
  }
})