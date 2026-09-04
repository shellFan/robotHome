const { communityApi, commentApi, likeApi } = require('../../api/index')
const { fromNow, imageOf, formatCount } = require('../../utils/format')
const { ensureLogin } = require('../../utils/request')
Page({
  data: { post: null, comments: [], commentText: '', liked: false, loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const post = await communityApi.detail(this.id)
      let comments = []
      try {
        const c = await commentApi.list({ bizType: 'post', bizId: this.id, page: 1, size: 50 })
        comments = (c && (c.records || c.list || c)) || []
        if (!Array.isArray(comments)) comments = []
      } catch (e) {}
      const images = post.images || post.imageList || []
      this.setData({
        post: Object.assign({}, post, {
          avatar: imageOf(post.avatar || post.userAvatar),
          timeText: fromNow(post.createTime),
          likeText: formatCount(post.likeCount),
          images: (Array.isArray(images) ? images : []).map(function (img) {
            return typeof img === 'string' ? imageOf(img) : imageOf(img.url || img.imageUrl)
          })
        }),
        comments: comments.map(function (x) {
          return Object.assign({}, x, {
            avatar: imageOf(x.avatar || x.userAvatar),
            timeText: fromNow(x.createTime)
          })
        }),
        loading: false
      })
    } catch (e) { this.setData({ loading: false }) }
  },
  preview(e) {
    wx.previewImage({ current: e.currentTarget.dataset.url, urls: this.data.post.images })
  },
  onCommentInput(e) { this.setData({ commentText: e.detail.value }) },
  async submitComment() {
    if (!ensureLogin()) return
    const content = (this.data.commentText || '').trim()
    if (!content) return wx.showToast({ title: '请输入评论', icon: 'none' })
    try {
      await commentApi.add({ bizType: 'post', bizId: this.id, content: content })
      this.setData({ commentText: '' })
      wx.showToast({ title: '评论成功', icon: 'success' })
      this.load()
    } catch (e) {}
  },
  async onLike() {
    if (!ensureLogin()) return
    try {
      await likeApi.toggle('post', this.id)
      this.setData({ liked: !this.data.liked })
    } catch (e) {}
  }
})
