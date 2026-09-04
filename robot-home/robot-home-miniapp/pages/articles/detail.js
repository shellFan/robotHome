const { articleApi, favoriteApi, likeApi, commentApi } = require('../../api/index')
const { formatDate, formatCount, imageOf, fromNow } = require('../../utils/format')
const { ensureLogin, getToken } = require('../../utils/request')
Page({
  data: { article: null, comments: [], commentText: '', favorited: false, liked: false, loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const article = await articleApi.detail(this.id)
      let comments = []
      try {
        const c = await commentApi.list({ bizType: 'article', bizId: this.id, page: 1, size: 20 })
        comments = (c && (c.records || c.list || c)) || []
        if (!Array.isArray(comments)) comments = []
        comments = comments.map(function (x) {
          return Object.assign({}, x, { timeText: fromNow(x.createTime), avatar: imageOf(x.avatar || x.userAvatar) })
        })
      } catch (e) {}
      this.setData({
        article: Object.assign({}, article, {
          cover: imageOf(article.cover),
          dateText: formatDate(article.publishTime, true),
          viewText: formatCount(article.viewCount),
          contentHtml: article.content || article.html || ''
        }),
        comments: comments,
        loading: false
      })
      wx.setNavigationBarTitle({ title: article.title || '资讯详情' })
    } catch (e) { this.setData({ loading: false }) }
  },
  onCommentInput(e) { this.setData({ commentText: e.detail.value }) },
  async submitComment() {
    if (!ensureLogin()) return
    const content = (this.data.commentText || '').trim()
    if (!content) return wx.showToast({ title: '请输入评论', icon: 'none' })
    try {
      await commentApi.add({ bizType: 'article', bizId: this.id, content: content })
      this.setData({ commentText: '' })
      wx.showToast({ title: '评论成功', icon: 'success' })
      this.load()
    } catch (e) {}
  },
  async onFavorite() {
    if (!ensureLogin()) return
    try {
      await favoriteApi.toggle('article', this.id)
      this.setData({ favorited: !this.data.favorited })
    } catch (e) {}
  },
  async onLike() {
    if (!ensureLogin()) return
    try {
      await likeApi.toggle('article', this.id)
      this.setData({ liked: !this.data.liked })
    } catch (e) {}
  }
})
