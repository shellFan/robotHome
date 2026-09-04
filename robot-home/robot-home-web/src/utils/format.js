import dayjs from 'dayjs'

/** 价格格式化：0 / 空显示为「暂无报价」 */
export function formatPrice (price) {
  if (price === null || price === undefined || Number(price) <= 0) {
    return '暂无报价'
  }
  const num = Number(price)
  if (num >= 10000) {
    const wan = num / 10000
    return '¥' + (num % 10000 === 0 ? String(Math.round(wan)) : wan.toFixed(2)) + '万'
  }
  return '¥' + num.toLocaleString('zh-CN')
}

/** 数字简写：12000 -> 1.2万 */
export function formatCount (n) {
  if (n === null || n === undefined) return '0'
  const num = Number(n)
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return String(num)
}

export function formatDate (value, pattern) {
  if (!value) return ''
  return dayjs(value).format(pattern || 'YYYY-MM-DD')
}

/** 相对时间 */
export function fromNow (value) {
  if (!value) return ''
  const target = dayjs(value)
  const diff = dayjs().diff(target, 'second')
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 2592000) return Math.floor(diff / 86400) + ' 天前'
  return target.format('YYYY-MM-DD')
}

/** 时长（秒）转 mm:ss */
export function formatDuration (seconds) {
  if (!seconds) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  const mm = String(m).padStart(2, '0')
  const ss = String(s).padStart(2, '0')
  return mm + ':' + ss
}

/** 解析后端 mainParams（JSON 字符串）为键值对数组 */
export function parseMainParams (json, limit) {
  if (!json) return []
  let obj = null
  try {
    obj = typeof json === 'string' ? JSON.parse(json) : json
  } catch (e) {
    return []
  }
  if (!obj) return []
  const max = limit || 4
  return Object.keys(obj).slice(0, max).map(function (k) {
    return { label: k, value: obj[k] }
  })
}

/** 图片地址兜底 */
export function placeholderImage () {
  const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">' +
    '<rect width="400" height="300" fill="#f2f3f5"/>' +
    '<text x="50%" y="50%" fill="#a9aeb8" font-size="16" text-anchor="middle" dominant-baseline="middle">暂无图片</text>' +
    '</svg>'
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}

const PLACEHOLDER_COLORS = ['#1668dc', '#2ba471', '#d9822b', '#c45656', '#6b4c9a', '#0d9488']

function hashSeed (s) {
  let h = 0
  for (let i = 0; i < s.length; i++) {
    h = (h * 31 + s.charCodeAt(i)) >>> 0
  }
  return h
}

function generatedImage (seed) {
  const color = PLACEHOLDER_COLORS[hashSeed(String(seed || 'robot')) % PLACEHOLDER_COLORS.length]
  const svg =
    '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600">' +
    '<rect width="800" height="600" fill="#eef3fb"/>' +
    '<rect width="800" height="8" fill="' + color + '"/>' +
    '<rect x="310" y="170" width="180" height="150" rx="28" fill="' + color + '"/>' +
    '<circle cx="360" cy="230" r="18" fill="#ffffff"/>' +
    '<circle cx="440" cy="230" r="18" fill="#ffffff"/>' +
    '<rect x="355" y="275" width="90" height="14" rx="7" fill="#ffffff"/>' +
    '<rect x="330" y="340" width="140" height="120" rx="18" fill="' + color + '"/>' +
    '<rect x="250" y="360" width="60" height="22" rx="11" fill="' + color + '"/>' +
    '<rect x="490" y="360" width="60" height="22" rx="11" fill="' + color + '"/>' +
    '</svg>'
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}

/** 渲染图片：空地址或外网占位图不可达时使用本地 SVG */
export function imageOf (url) {
  if (!url) return placeholderImage()
  if (/picsum\.photos|loremflickr|unsplash\.com/i.test(url)) {
    const match = url.match(/seed\/([^/]+)/)
    return generatedImage(match ? match[1] : 'robot')
  }
  return url
}
