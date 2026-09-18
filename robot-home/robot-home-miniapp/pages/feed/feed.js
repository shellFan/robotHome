const { feedApi } = require('../../api/index')
const { formatDate, imageOf } = require('../../utils/format')

Page({
  data: {
    list: [],
    loading: true,
    finished: false,
    lastId: null
  },
  onLoad() {
    this.load()
  },
  onReachBottom() {
    this.load()
  },
  onPullDownRefresh() {
    this.setData({ list: [], lastId: null, finished: false })
    this.load().then(function () { wx.stopPullDownRefresh() })
  },
  async load() {
    if (this.data.loading && this.data.list.length) return
    if (this.data.finished) return
    this.setData({ loading: true })
    try {
      var data = await feedApi.mine(this.data.lastId, 20)
      var records = (data && data.records) || (data && data.list) || []
      if (!Array.isArray(records)) records = []
      records = records.map(function (item) {
        return Object.assign({}, item, {
          cover: imageOf(item.cover || item.imageUrl),
          dateText: formatDate(item.createdAt)
        })
      })
      var lastItem = records.length ? records[records.length - 1] : null
      var newList = this.data.list.concat(records)
      this.setData({
        list: newList,
        lastId: lastItem ? lastItem.id : this.data.lastId,
        finished: records.length < 20,
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  goDetail(e) {
    var item = e.currentTarget.dataset.item
    if (!item) return
    var type = item.contentType || item.type || ''
    if (type === 'ROBOT' || type === 'robot') {
      wx.navigateTo({ url: '/pages/robots/detail?id=' + item.targetId })
    } else if (type === 'BRAND' || type === 'brand') {
      wx.navigateTo({ url: '/pages/brands/detail?id=' + item.targetId })
    } else if (type === 'ARTICLE' || type === 'article') {
      wx.navigateTo({ url: '/pages/articles/detail?id=' + item.targetId })
    }
  }
})