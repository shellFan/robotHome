const { placeholderImage } = require('./config')

function formatPrice(price) {
  if (price === null || price === undefined || Number(price) <= 0) return '暂无报价'
  const num = Number(price)
  if (num >= 10000) {
    const wan = num / 10000
    return '¥' + (num % 10000 === 0 ? String(Math.round(wan)) : wan.toFixed(2)) + '万'
  }
  return '¥' + num.toLocaleString('zh-CN')
}

function formatCount(n) {
  if (n === null || n === undefined) return '0'
  const num = Number(n)
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return String(num)
}

function pad(n) {
  return n < 10 ? '0' + n : String(n)
}

function formatDate(value, withTime) {
  if (!value) return ''
  const d = new Date(typeof value === 'number' ? value : String(value).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(value).slice(0, 10)
  const base = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
  if (!withTime) return base
  return base + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

function fromNow(value) {
  if (!value) return ''
  const target = new Date(typeof value === 'number' ? value : String(value).replace(/-/g, '/'))
  if (Number.isNaN(target.getTime())) return ''
  const diff = Math.floor((Date.now() - target.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 2592000) return Math.floor(diff / 86400) + ' 天前'
  return formatDate(value)
}

function formatDuration(seconds) {
  if (!seconds) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return pad(m) + ':' + pad(s)
}

function parseMainParams(json, limit) {
  if (!json) return []
  let obj = null
  try {
    obj = typeof json === 'string' ? JSON.parse(json) : json
  } catch (e) {
    return []
  }
  if (!obj || typeof obj !== 'object') return []
  const max = limit || 4
  return Object.keys(obj).slice(0, max).map(function (k) {
    return { label: k, value: obj[k] }
  })
}

function imageOf(url) {
  return url || placeholderImage
}

function unwrapList(data) {
  if (!data) return []
  if (Array.isArray(data)) return data
  if (Array.isArray(data.records)) return data.records
  if (Array.isArray(data.list)) return data.list
  return []
}

function pageTotal(data) {
  if (!data) return 0
  if (typeof data.total === 'number') return data.total
  if (Array.isArray(data)) return data.length
  return 0
}

module.exports = {
  formatPrice,
  formatCount,
  formatDate,
  fromNow,
  formatDuration,
  parseMainParams,
  imageOf,
  unwrapList,
  pageTotal
}
