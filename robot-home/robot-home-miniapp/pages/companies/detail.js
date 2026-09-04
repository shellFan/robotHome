const { companyApi } = require('../../api/index')
const { formatPrice, imageOf, unwrapList } = require('../../utils/format')
Page({
  data: { company: null, robots: [], loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const company = await companyApi.detail(this.id)
      const robotsData = await companyApi.robots(this.id, { page: 1, size: 50 })
      this.setData({
        company: Object.assign({}, company, { logo: imageOf(company.logo) }),
        robots: unwrapList(robotsData).map(function (r) {
          return Object.assign({}, r, { cover: imageOf(r.cover || r.mainImage), priceText: formatPrice(r.guidePrice) })
        }),
        loading: false
      })
      wx.setNavigationBarTitle({ title: company.name || '企业详情' })
    } catch (e) { this.setData({ loading: false }) }
  },
  goRobot(e) { wx.navigateTo({ url: '/pages/robots/detail?id=' + e.currentTarget.dataset.id }) }
})
