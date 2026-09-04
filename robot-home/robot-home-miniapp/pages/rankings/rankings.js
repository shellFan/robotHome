const { rankingApi } = require('../../api/index')
const { formatPrice, imageOf } = require('../../utils/format')

Page({
  data: { types: [], type: 'hot', list: [], loading: true },
  onLoad(q) {
    this.setData({ type: q.type || 'hot' })
    this.loadTypes()
    this.load()
  },
  async loadTypes() {
    try {
      const types = await rankingApi.types()
      const arr = Array.isArray(types) ? types : (types && types.list) || []
      this.setData({ types: arr.length ? arr : [
        { code: 'hot', name: '热门榜' },
        { code: 'new', name: '新品榜' },
        { code: 'sales', name: '热销榜' }
      ]})
    } catch (e) {
      this.setData({ types: [
        { code: 'hot', name: '热门榜' },
        { code: 'new', name: '新品榜' }
      ]})
    }
  },
  async load() {
    this.setData({ loading: true })
    try {
      const data = await rankingApi.rank(this.data.type, 50)
      let list = []
      if (Array.isArray(data)) list = data
      else if (data && data.robots) list = data.robots
      else if (data && data.list) list = data.list
      list = list.map(function (r, i) {
        return Object.assign({}, r, {
          rank: i + 1,
          cover: imageOf(r.cover || r.mainImage),
          priceText: formatPrice(r.guidePrice)
        })
      })
      this.setData({ list: list, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  },
  switchType(e) {
    this.setData({ type: e.currentTarget.dataset.code })
    this.load()
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id }) }
})
