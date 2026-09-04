const { robotApi } = require('../../api/index')
Page({
  data: { groups: [], loading: true },
  onLoad(q) { this.id = q.id; this.load() },
  async load() {
    try {
      const data = await robotApi.params(this.id)
      let groups = []
      if (Array.isArray(data)) {
        groups = data.map(function (g) {
          return {
            name: g.groupName || g.name || '参数',
            items: (g.items || g.params || []).map(function (it) {
              return { label: it.name || it.label || it.paramName, value: it.value || it.paramValue || '-' }
            })
          }
        })
      } else if (data && typeof data === 'object') {
        groups = Object.keys(data).map(function (k) {
          const v = data[k]
          if (Array.isArray(v)) {
            return { name: k, items: v.map(function (it) {
              return { label: it.name || it.label, value: it.value || '-' }
            }) }
          }
          return { name: k, items: Object.keys(v || {}).map(function (kk) { return { label: kk, value: v[kk] } }) }
        })
      }
      this.setData({ groups: groups, loading: false })
    } catch (e) { this.setData({ loading: false }) }
  }
})
