const { enterpriseApi } = require('../../api/index')
const { ensureLogin, getToken } = require('../../utils/request')

Page({
  data: {
    companyId: '',
    companyName: '',
    applicantName: '',
    applicantPhone: '',
    applicantEmail: '',
    applicantPosition: '',
    claimReason: '',
    proofMaterials: '',
    latestClaim: null,
    loading: true,
    error: '',
    blocked: false,
    submitting: false
  },
  onLoad: function (q) {
    var companyId = String(q.companyId || '').trim()
    var companyName = q.companyName || ''
    try {
      companyName = decodeURIComponent(companyName)
    } catch (e) {}
    this.companyId = companyId
    this.setData({ companyId: companyId, companyName: companyName })
    if (!/^\d+$/.test(companyId) || companyId === '0') {
      this.setData({ loading: false, error: '企业信息无效' })
      return
    }
    this.pageReady = true
    if (!ensureLogin()) return
    this.loadStatus()
  },
  onShow: function () {
    if (this.pageReady && !this.loaded && getToken()) this.loadStatus()
  },
  loadStatus: async function () {
    if (this.loadingStatus) return
    this.loadingStatus = true
    this.loaded = true
    this.setData({ loading: true, error: '' })
    try {
      var latestClaim = await enterpriseApi.latestClaim(this.companyId)
      var status = latestClaim && latestClaim.status
      this.setData({
        latestClaim: latestClaim || null,
        blocked: status === 'PENDING' || status === 'APPROVED',
        loading: false
      })
    } catch (e) {
      this.loaded = false
      this.setData({ loading: false, error: '认领状态加载失败，请重试' })
    } finally {
      this.loadingStatus = false
    }
  },
  retry: function () {
    if (!ensureLogin()) return
    this.loadStatus()
  },
  onName: function (e) { this.setData({ applicantName: e.detail.value }) },
  onPhone: function (e) { this.setData({ applicantPhone: e.detail.value }) },
  onEmail: function (e) { this.setData({ applicantEmail: e.detail.value }) },
  onPosition: function (e) { this.setData({ applicantPosition: e.detail.value }) },
  onReason: function (e) { this.setData({ claimReason: e.detail.value }) },
  onProofMaterials: function (e) { this.setData({ proofMaterials: e.detail.value }) },
  goClaims: function () {
    wx.redirectTo({ url: '/pages/workspace/claims' })
  },
  submit: async function () {
    if (!ensureLogin()) return
    if (this.data.submitting || this.data.loading || this.data.blocked) return
    var applicantName = (this.data.applicantName || '').trim()
    var applicantPhone = (this.data.applicantPhone || '').trim()
    var applicantEmail = (this.data.applicantEmail || '').trim()
    var applicantPosition = (this.data.applicantPosition || '').trim()
    var claimReason = (this.data.claimReason || '').trim()
    var proofLinks = (this.data.proofMaterials || '').split(/\r?\n/).map(function (value) {
      return value.trim()
    }).filter(function (value) { return !!value })
    if (!applicantName) return wx.showToast({ title: '请填写申请人姓名', icon: 'none' })
    if (!applicantPhone) return wx.showToast({ title: '请填写联系电话', icon: 'none' })
    if (!claimReason) return wx.showToast({ title: '请填写认领理由', icon: 'none' })
    var payload = {
      companyId: this.companyId,
      applicantName: applicantName,
      applicantPhone: applicantPhone,
      claimReason: claimReason
    }
    if (applicantEmail) payload.applicantEmail = applicantEmail
    if (applicantPosition) payload.applicantPosition = applicantPosition
    if (proofLinks.length) payload.proofMaterials = JSON.stringify(proofLinks)
    this.setData({ submitting: true })
    try {
      await enterpriseApi.submitClaim(payload)
      this.setData({ blocked: true, latestClaim: { status: 'PENDING' } })
      wx.showToast({ title: '认领申请已提交', icon: 'success' })
      setTimeout(function () {
        wx.redirectTo({ url: '/pages/workspace/claims' })
      }, 600)
    } catch (e) {
      this.setData({ submitting: false })
    }
  }
})
