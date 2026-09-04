const { compareKey, compareMax } = require('./config')

function getCompareIds() {
  try {
    const ids = wx.getStorageSync(compareKey)
    return Array.isArray(ids) ? ids.map(Number).filter(Boolean) : []
  } catch (e) {
    return []
  }
}

function setCompareIds(ids) {
  wx.setStorageSync(compareKey, ids.slice(0, compareMax))
}

function toggleCompare(id) {
  const nid = Number(id)
  let ids = getCompareIds()
  const idx = ids.indexOf(nid)
  if (idx >= 0) {
    ids.splice(idx, 1)
    setCompareIds(ids)
    return { added: false, ids: ids }
  }
  if (ids.length >= compareMax) {
    wx.showToast({ title: '最多对比 ' + compareMax + ' 款', icon: 'none' })
    return { added: false, ids: ids, full: true }
  }
  ids.push(nid)
  setCompareIds(ids)
  return { added: true, ids: ids }
}

function clearCompare() {
  setCompareIds([])
}

module.exports = {
  getCompareIds,
  setCompareIds,
  toggleCompare,
  clearCompare
}
