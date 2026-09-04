const { articleApi, videoApi, tutorialApi } = require('../../api/index')
const { formatDate, formatCount, formatDuration, imageOf, unwrapList } = require('../../utils/format')

Page({
  data: {
    tab: 'articles',
    articles: [], videos: [], tutorials: [],
    page: 1, finished: false, loading: false
  },
  onLoad() { this.reload() },
  onReachBottom() { this.more() },
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab === this.data.tab) return
    this.setData({ tab: tab })
    this.reload()
  },
  async reload() {
    this.setData({ page: 1, articles: [], videos: [], tutorials: [], finished: false })
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
      const tab = this.data.tab
      let data
      if (tab === 'articles') data = await articleApi.page({ page: this.data.page, size: 10 })
      else if (tab === 'videos') data = await videoApi.page({ page: this.data.page, size: 10 })
      else data = await tutorialApi.page({ page: this.data.page, size: 10 })
      let records = unwrapList(data)
      if (tab === 'articles') {
        records = records.map(function (a) {
          return Object.assign({}, a, {
            cover: imageOf(a.cover),
            dateText: formatDate(a.publishTime),
            viewText: formatCount(a.viewCount)
          })
        })
        const list = append ? this.data.articles.concat(records) : records
        this.setData({ articles: list, finished: records.length < 10, loading: false })
      } else if (tab === 'videos') {
        records = records.map(function (v) {
          return Object.assign({}, v, {
            cover: imageOf(v.cover),
            durationText: formatDuration(v.duration),
            viewText: formatCount(v.viewCount)
          })
        })
        const list = append ? this.data.videos.concat(records) : records
        this.setData({ videos: list, finished: records.length < 10, loading: false })
      } else {
        records = records.map(function (t) {
          return Object.assign({}, t, {
            cover: imageOf(t.cover),
            dateText: formatDate(t.publishTime || t.createTime)
          })
        })
        const list = append ? this.data.tutorials.concat(records) : records
        this.setData({ tutorials: list, finished: records.length < 10, loading: false })
      }
    } catch (e) { this.setData({ loading: false }) }
  },
  goArticle(e) { wx.navigateTo({ url: '/pages/articles/detail?id=' + e.currentTarget.dataset.id }) },
  goVideo(e) { wx.navigateTo({ url: '/pages/videos/detail?id=' + e.currentTarget.dataset.id }) },
  goTutorial(e) { wx.navigateTo({ url: '/pages/tutorials/detail?id=' + e.currentTarget.dataset.id }) },
  goMore() {
    const map = { articles: '/pages/articles/list', videos: '/pages/videos/list', tutorials: '/pages/tutorials/list' }
    wx.navigateTo({ url: map[this.data.tab] })
  }
})
