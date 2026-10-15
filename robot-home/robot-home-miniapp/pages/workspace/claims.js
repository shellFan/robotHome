const { enterpriseApi } = require('../../api/index')
const { ensureLogin, getToken } = require('../../utils/request')
const { formatDate } = require('../../utils/format')

var STATUS_TEXT = {
  PENDING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELLED: '已取消'
}

function mapClaim(item) {
  return Object.assign({}, item, {
    statusText: STATUS_TEXT[item.status] || item.status || '未知状态',
    createTimeText: formatDate(item.createTime, true)
  })
}

Page({
  data: {
    list: [],
    pageNum: 0,
    pageSize: 10,
    loading: true,
    loadingMore: false,
    finished: false,
    error: '',
    cancelingId: null
  },
  onLoad: function () {
    this.pageReady = true
    if (!ensureLogin()) return
    this.reload()
  },
  onShow: function () {
    if (this.pageReady && !this.loaded && getToken()) this.reload()
  },
  onPullDownRefresh: function () {
    this.reload()
  },
  onReachBottom: function () {
    if (!this.data.loading && !this.data.loadingMore && !this.data.finished) {
      this.fetchPage(this.data.pageNum + 1, true)
    }
  },
  reload: function () {
    if (!ensureLogin() || this.fetching) {
      wx.stopPullDownRefresh()
      return
    }
    this.loaded = true
    this.fetchPage(1, false)
  },
  retry: function () {
    this.reload()
  },
  fetchPage: async function (pageNum, append) {
    if (this.fetching) return
    this.fetching = true
    if (append) {
      this.setData({ loadingMore: true })
    } else {
      this.setData({ loading: true, error: '', finished: false })
    }
    try {
      var res = await enterpriseApi.myClaims({ pageNum: pageNum, pageSize: this.data.pageSize })
      var records = res && Array.isArray(res.list) ? res.list.map(mapClaim) : []
      var list = append ? this.data.list.concat(records) : records
      var total = res && typeof res.total === 'number' ? res.total : list.length
      var pages = res && typeof res.pages === 'number' ? res.pages : 0
      this.setData({
        list: list,
        pageNum: pageNum,
        loading: false,
        loadingMore: false,
        finished: pages ? pageNum >= pages : (list.length >= total || records.length < this.data.pageSize),
        error: ''
      })
    } catch (e) {
      if (!append) this.loaded = false
      this.setData({
        loading: false,
        loadingMore: false,
        error: append ? '' : '认领记录加载失败，请重试'
      })
    } finally {
      this.fetching = false
      wx.stopPullDownRefresh()
    }
  },
  cancelClaim: function (e) {
    var claimId = e.currentTarget.dataset.id
    if (!claimId || this.data.cancelingId) return
    var that = this
    wx.showModal({
      title: '取消认领申请',
      content: '取消后，该申请将不再进入审核流程。',
      confirmText: '确认取消',
      confirmColor: '#f5222d',
      success: function (res) {
        if (res.confirm) that.doCancel(claimId)
      }
    })
  },
  doCancel: async function (claimId) {
    if (this.data.cancelingId) return
    this.setData({ cancelingId: claimId })
    try {
      await enterpriseApi.cancelClaim(claimId)
      this.setData({
        list: this.data.list.map(function (item) {
          return String(item.id) === String(claimId) ? Object.assign({}, item, { status: 'CANCELLED', statusText: STATUS_TEXT.CANCELLED }) : item
        }),
        cancelingId: null
      })
      wx.showToast({ title: '申请已取消', icon: 'success' })
    } catch (e) {
      this.setData({ cancelingId: null })
    }
  }
})
