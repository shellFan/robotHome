const { reviewApi } = require('../../api/index')
const { ensureLogin, getToken } = require('../../utils/request')

Page({
  data: {
    id: '',
    summary: null,
    reviews: [],
    pageNum: 1,
    pageSize: 20,
    total: 0,
    loading: false,
    hasMore: true,
    // Submit form
    showForm: false,
    submitting: false,
    form: { overallScore: 0, qualityScore: 0, serviceScore: 0, costScore: 0, content: '', images: '' },
    rateTexts: ['很差', '较差', '一般', '较好', '很好']
  },
  onLoad(q) {
    this.setData({ id: q.id })
    this.loadSummary()
    this.loadReviews()
  },
  async loadSummary() {
    try {
      const data = await reviewApi.summary(this.data.id)
      this.setData({ summary: data })
    } catch (e) {}
  },
  async loadReviews() {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const data = await reviewApi.list(this.data.id, this.data.pageNum, this.data.pageSize)
      const list = data.records || data.list || []
      this.setData({
        reviews: this.data.reviews.concat(list),
        total: data.total || 0,
        hasMore: this.data.reviews.length + list.length < (data.total || 0),
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  loadMore() {
    if (!this.data.hasMore || this.data.loading) return
    this.setData({ pageNum: this.data.pageNum + 1 })
    this.loadReviews()
  },
  formatScore(val) {
    if (val == null) return '-'
    return Number(val).toFixed(1)
  },
  scorePercent(count) {
    const s = this.data.summary
    if (!s || !s.reviewCount) return 0
    return Math.round((count / s.reviewCount) * 100)
  },
  // Submit review
  openForm() {
    if (!ensureLogin()) return
    this.setData({ showForm: true, form: { overallScore: 0, qualityScore: 0, serviceScore: 0, costScore: 0, content: '', images: '' } })
  },
  closeForm() { this.setData({ showForm: false }) },
  onStarTap(e) {
    const field = e.currentTarget.dataset.field
    const value = e.currentTarget.dataset.value
    const form = Object.assign({}, this.data.form)
    form[field] = value
    this.setData({ form: form })
  },
  onContent(e) {
    const form = Object.assign({}, this.data.form)
    form.content = e.detail.value
    this.setData({ form: form })
  },
  async submitReview() {
    const f = this.data.form
    if (!f.overallScore) { wx.showToast({ title: '请完成综合评分', icon: 'none' }); return }
    if (!f.content || f.content.length < 10) { wx.showToast({ title: '评价内容至少10个字', icon: 'none' }); return }
    this.setData({ submitting: true })
    try {
      await reviewApi.submit({
        robotId: Number(this.data.id),
        overallScore: f.overallScore,
        qualityScore: f.qualityScore || f.overallScore,
        serviceScore: f.serviceScore || f.overallScore,
        costScore: f.costScore || f.overallScore,
        content: f.content,
        images: f.images
      })
      wx.showToast({ title: '评价已提交', icon: 'success' })
      this.setData({ showForm: false, reviews: [], pageNum: 1 })
      this.loadSummary()
      this.loadReviews()
    } catch (e) {
      wx.showToast({ title: e.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
  async markHelpful(e) {
    if (!ensureLogin()) return
    const id = e.currentTarget.dataset.id
    try {
      await reviewApi.helpful(id)
      wx.showToast({ title: '感谢反馈', icon: 'none' })
    } catch (e) {
      wx.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  }
})