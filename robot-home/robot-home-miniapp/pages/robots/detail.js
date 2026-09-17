const { robotApi, favoriteApi, behaviorApi, reviewApi, similarApi, correctionApi } = require('../../api/index')
const { formatPrice, formatCount, imageOf, parseMainParams } = require('../../utils/format')
const { toggleCompare, getCompareIds } = require('../../utils/compare')
const { ensureLogin, getToken } = require('../../utils/request')

Page({
  data: {
    id: null, robot: null, params: [], favorited: false, inCompare: false, loading: true,
    // Phase7
    reviewSummary: null, similarRobots: [],
    showCorrection: false, correctionForm: { defId: '', newValue: '', reason: '' },
    showProcure: false, procureType: 'PRICE',
    procureForm: { name: '', phone: '', region: '', customerType: 1, companyName: '', quantity: 1, budget: '', remark: '', procurementScene: '', purchaseTime: '' },
    procureTypeLabels: { PRICE: '获取底价', PURCHASE: '我要采购', LEASE: '租赁咨询', COOPERATE: '合作洽谈' },
    sceneOptions: ['工业制造', '物流仓储', '医疗健康', '教育培训', '服务行业', '其他'],
    sceneValues: ['INDUSTRIAL', 'LOGISTICS', 'MEDICAL', 'EDUCATION', 'SERVICE', 'OTHER']
  },
  onLoad(q) {
    this.setData({ id: q.id })
    this.load()
  },
  async load() {
    this.setData({ loading: true })
    try {
      const robot = await robotApi.detail(this.data.id)
      robotApi.view(this.data.id).catch(function () {})
      behaviorApi.track('view', 'robot', Number(this.data.id))
      const ids = getCompareIds()
      let favorited = false
      if (getToken()) {
        try {
          const c = await favoriteApi.check('robot', this.data.id)
          favorited = !!(c === true || (c && c.favorited) || (c && c.liked))
        } catch (e) {}
      }
      this.setData({
        robot: Object.assign({}, robot, {
          cover: imageOf(robot.cover || robot.mainImage),
          priceText: formatPrice(robot.guidePrice),
          viewText: formatCount(robot.viewCount)
        }),
        params: parseMainParams(robot.mainParams, 6),
        favorited: favorited,
        inCompare: ids.indexOf(Number(this.data.id)) >= 0,
        loading: false
      })
      wx.setNavigationBarTitle({ title: robot.name || '机器人详情' })
      // Phase7: load review summary & similar
      this.loadReviewSummary()
      this.loadSimilarRobots()
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  // Phase7: Review Summary
  async loadReviewSummary() {
    try {
      const data = await reviewApi.summary(this.data.id)
      this.setData({ reviewSummary: data })
    } catch (e) {}
  },
  // Phase7: Similar Robots
  async loadSimilarRobots() {
    try {
      const data = await similarApi.list(this.data.id, 6)
      this.setData({ similarRobots: data || [] })
    } catch (e) {}
  },
  formatScore(val) {
    if (val == null) return '-'
    return Number(val).toFixed(1)
  },
  scorePercent(count) {
    const s = this.data.reviewSummary
    if (!s || !s.reviewCount) return 0
    return Math.round((count / s.reviewCount) * 100)
  },
  goParams() { wx.navigateTo({ url: '/pages/robots/params?id=' + this.data.id }) },
  goImages() { wx.navigateTo({ url: '/pages/robots/images?id=' + this.data.id }) },
  goVideos() { wx.navigateTo({ url: '/pages/robots/videos?id=' + this.data.id }) },
  goReviews() { wx.navigateTo({ url: '/pages/robots/reviews?id=' + this.data.id }) },
  goInquiry() {
    behaviorApi.track('inquiry', 'robot', Number(this.data.id))
    wx.navigateTo({ url: '/pages/inquiry/inquiry?robotId=' + this.data.id + '&robotName=' + encodeURIComponent((this.data.robot && this.data.robot.name) || '') })
  },
  // Phase7: Procurement V2
  openProcure(e) {
    if (!ensureLogin()) return
    const type = e.currentTarget.dataset.type || 'PRICE'
    this.setData({ showProcure: true, procureType: type, procureForm: { name: '', phone: '', region: '', customerType: 1, companyName: '', quantity: 1, budget: '', remark: '', procurementScene: '', purchaseTime: '' } })
  },
  closeProcure() { this.setData({ showProcure: false }) },
  onProcureInput(e) {
    const field = e.currentTarget.dataset.field
    const form = Object.assign({}, this.data.procureForm)
    form[field] = e.detail.value
    this.setData({ procureForm: form })
  },
  onSceneChange(e) {
    const form = Object.assign({}, this.data.procureForm)
    form.procurementScene = this.data.sceneValues[e.detail.value] || ''
    this.setData({ procureForm: form })
  },
  async submitProcure() {
    const f = this.data.procureForm
    if (!f.name.trim()) { wx.showToast({ title: '请填写姓名', icon: 'none' }); return }
    if (!/^1[3-9]\d{9}$/.test(f.phone)) { wx.showToast({ title: '手机号格式不正确', icon: 'none' }); return }
    try {
      const { inquiryApi } = require('../../api/index')
      await inquiryApi.submit({
        robotId: Number(this.data.id),
        name: f.name.trim(), phone: f.phone.trim(), region: f.region,
        customerType: f.customerType, companyName: f.companyName,
        quantity: f.quantity, budget: f.budget, remark: f.remark,
        inquiryType: this.data.procureType,
        procurementScene: f.procurementScene, purchaseTime: f.purchaseTime
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      this.setData({ showProcure: false })
    } catch (e) {
      wx.showToast({ title: e.message || '提交失败', icon: 'none' })
    }
  },
  // Phase7: Correction
  openCorrection() {
    if (!ensureLogin()) return
    this.setData({ showCorrection: true, correctionForm: { defId: '', newValue: '', reason: '' } })
  },
  closeCorrection() { this.setData({ showCorrection: false }) },
  onCorrectionInput(e) {
    const field = e.currentTarget.dataset.field
    const form = Object.assign({}, this.data.correctionForm)
    form[field] = e.detail.value
    this.setData({ correctionForm: form })
  },
  async submitCorrection() {
    const f = this.data.correctionForm
    if (!f.newValue.trim()) { wx.showToast({ title: '请填写正确值', icon: 'none' }); return }
    if (!f.reason || f.reason.length < 5) { wx.showToast({ title: '理由至少5个字', icon: 'none' }); return }
    try {
      await correctionApi.submit({
        robotId: Number(this.data.id),
        defId: f.defId ? Number(f.defId) : null,
        newValue: f.newValue.trim(),
        reason: f.reason.trim()
      })
      wx.showToast({ title: '纠错已提交', icon: 'success' })
      this.setData({ showCorrection: false })
    } catch (e) {
      wx.showToast({ title: e.message || '提交失败', icon: 'none' })
    }
  },
  onCompare() {
    const r = toggleCompare(this.data.id)
    if (!r.full) {
      this.setData({ inCompare: r.added || r.ids.indexOf(Number(this.data.id)) >= 0 })
      behaviorApi.track(r.added ? 'add_compare' : 'remove_compare', 'robot', Number(this.data.id))
      wx.showToast({ title: r.added ? '已加入对比' : '已移出对比', icon: 'none' })
    }
  },
  goComparePage() { wx.navigateTo({ url: '/pages/compare/compare' }) },
  async onFavorite() {
    if (!ensureLogin()) return
    try {
      await favoriteApi.toggle('robot', this.data.id)
      const next = !this.data.favorited
      this.setData({ favorited: next })
      behaviorApi.track(next ? 'favorite' : 'unfavorite', 'robot', Number(this.data.id))
      wx.showToast({ title: next ? '已收藏' : '已取消', icon: 'none' })
    } catch (e) {}
  },
  previewCover() {
    if (this.data.robot && this.data.robot.cover) {
      wx.previewImage({ urls: [this.data.robot.cover] })
    }
  },
  goSimilarRobot(e) {
    const id = e.currentTarget.dataset.id
    wx.redirectTo({ url: '/pages/robots/detail?id=' + id })
  }
})