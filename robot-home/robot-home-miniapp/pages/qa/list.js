const { qaApi } = require('../../api/index')
const { fromNow, imageOf, unwrapList, formatCount } = require('../../utils/format')
const { ensureLogin } = require('../../utils/request')

Page({
  data: {
    sort: 'latest',
    sortOptions: [
      { value: 'latest', label: '最新' },
      { value: 'hot', label: '最热' },
      { value: 'unanswered', label: '待回答' }
    ],
    questions: [],
    page: 1,
    finished: false,
    loading: false,
    showAsk: false,
    askTitle: '',
    askContent: ''
  },
  onLoad() { this.reload() },
  onReachBottom() { this.more() },
  onPullDownRefresh() {
    this.reload().finally(function () { wx.stopPullDownRefresh() })
  },
  switchSort(e) {
    const sort = e.currentTarget.dataset.sort
    if (sort === this.data.sort) return
    this.setData({ sort: sort })
    this.reload()
  },
  async reload() {
    this.setData({ page: 1, questions: [], finished: false })
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
      const data = await qaApi.questions({
        sort: this.data.sort,
        pageNum: this.data.page,
        pageSize: 10
      })
      const records = unwrapList(data).map(function (q) {
        return Object.assign({}, q, {
          avatar: imageOf(q.authorAvatar),
          timeText: fromNow(q.createTime),
          answerText: formatCount(q.answerCount),
          followText: formatCount(q.followCount),
          viewText: formatCount(q.viewCount),
          statusText: q.hasAccepted ? '已采纳' : (q.answerCount > 0 ? '已回答' : '待回答')
        })
      })
      const list = append ? this.data.questions.concat(records) : records
      this.setData({ questions: list, finished: records.length < 10, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  goDetail(e) {
    wx.navigateTo({ url: '/pages/qa/detail?id=' + e.currentTarget.dataset.id })
  },
  openAsk() {
    if (!ensureLogin()) return
    this.setData({ showAsk: true })
  },
  closeAsk() { this.setData({ showAsk: false }) },
  onAskTitle(e) { this.setData({ askTitle: e.detail.value }) },
  onAskContent(e) { this.setData({ askContent: e.detail.value }) },
  async submitAsk() {
    const title = (this.data.askTitle || '').trim()
    const content = (this.data.askContent || '').trim()
    if (!title) return wx.showToast({ title: '请输入标题', icon: 'none' })
    try {
      await qaApi.ask({ title: title, content: content })
      wx.showToast({ title: '提问成功', icon: 'success' })
      this.setData({ showAsk: false, askTitle: '', askContent: '' })
      this.reload()
    } catch (e) {}
  }
})