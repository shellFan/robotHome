const { brandApi } = require('../../api/index')
const { formatPrice, imageOf, unwrapList } = require('../../utils/format')
Page({
  data: { brand: null, robots: [], loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const brand = await brandApi.detail(this.id)
      const robotsData = await brandApi.robots(this.id, { page: 1, size: 50 })
      this.setData({
        brand: Object.assign({}, brand, { logo: imageOf(brand.logo) }),
        robots: unwrapList(robotsData).map(function (r) {
          return Object.assign({}, r, { cover: imageOf(r.cover || r.mainImage), priceText: formatPrice(r.guidePrice) })
        }),
        loading: false
      })
      wx.setNavigationBarTitle({ title: brand.name || '品牌详情' })
    } catch (e) { this.setData({ loading: false }) }
  },
  goRobot(e) { wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id }) }
})
