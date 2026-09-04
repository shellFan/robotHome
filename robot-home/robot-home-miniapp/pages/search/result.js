const { searchApi } = require('../../api/index')
const { formatPrice, imageOf, unwrapList } = require('../../utils/format')
Page({
  data: {
    keyword: '', tab: 'robot',
    robots: [], brands: [], articles: [], videos: [],
    loading: false
  },
  onLoad(q) {
    this.setData({ keyword: decodeURIComponent(q.keyword || '') })
    this.search()
  },
  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    this.search()
  },
  async search() {
    const keyword = this.data.keyword
    if (!keyword) return
    this.setData({ loading: true })
    try {
      // 综合搜索
      const all = await searchApi.search(keyword, 20)
      if (all && typeof all === 'object' && !Array.isArray(all)) {
        this.setData({
          robots: (all.robots || []).map(mapRobot),
          brands: (all.brands || []).map(mapBrand),
          articles: (all.articles || []).map(mapArticle),
          videos: (all.videos || []).map(mapVideo),
          loading: false
        })
        return
      }
    } catch (e) {}
    // 按类型兜底
    try {
      const type = this.data.tab === 'robot' ? 'robots' : (this.data.tab + 's')
      const data = await searchApi.byType(this.data.tab === 'robot' ? 'robot' : this.data.tab, { keyword: keyword, page: 1, size: 20 })
      const list = unwrapList(data)
      const patch = { loading: false }
      if (this.data.tab === 'robot') patch.robots = list.map(mapRobot)
      if (this.data.tab === 'brand') patch.brands = list.map(mapBrand)
      if (this.data.tab === 'article') patch.articles = list.map(mapArticle)
      if (this.data.tab === 'video') patch.videos = list.map(mapVideo)
      this.setData(patch)
    } catch (e) {
      this.setData({ loading: false })
    }
  },
  goRobot(e) { wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id }) },
  goBrand(e) { wx.navigateTo({ url: '/pages/brands/detail?id=' + e.currentTarget.dataset.id }) },
  goArticle(e) { wx.navigateTo({ url: '/pages/articles/detail?id=' + e.currentTarget.dataset.id }) },
  goVideo(e) { wx.navigateTo({ url: '/pages/videos/detail?id=' + e.currentTarget.dataset.id }) }
})
function mapRobot(r) { return Object.assign({}, r, { cover: imageOf(r.cover || r.mainImage), priceText: formatPrice(r.guidePrice) }) }
function mapBrand(b) { return Object.assign({}, b, { logo: imageOf(b.logo) }) }
function mapArticle(a) { return Object.assign({}, a, { cover: imageOf(a.cover) }) }
function mapVideo(v) { return Object.assign({}, v, { cover: imageOf(v.cover) }) }
