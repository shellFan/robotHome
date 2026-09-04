/**
 * 通用工具方法
 */

/** 把扁平数组转换成树（parentId 为 0 / null 视为根节点） */
export function buildTree(list, idKey = 'id', parentKey = 'parentId') {
  const arr = Array.isArray(list) ? list : []
  const map = new Map()
  const roots = []
  arr.forEach((item) => {
    map.set(item[idKey], Object.assign({ children: [] }, item))
  })
  arr.forEach((item) => {
    const node = map.get(item[idKey])
    const parent = map.get(item[parentKey])
    if (parent && parent[idKey] !== item[idKey]) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  const trim = (nodes) =>
    nodes.map((n) => {
      const copy = Object.assign({}, n)
      if (!copy.children || !copy.children.length) {
        delete copy.children
      } else {
        copy.children = trim(copy.children)
      }
      return copy
    })
  return trim(roots)
}

/** 在树中查找节点 */
export function findTreeNode(tree, predicate) {
  if (!Array.isArray(tree)) return null
  for (const node of tree) {
    if (predicate(node)) return node
    if (node.children && node.children.length) {
      const found = findTreeNode(node.children, predicate)
      if (found) return found
    }
  }
  return null
}

/** 把树拍平成带层级缩进标签的数组，供 el-select 使用 */
export function flattenTree(tree, level = 0, result = []) {
  ;(tree || []).forEach((node) => {
    const label = level > 0 ? `${'　'.repeat(level)}└ ${node.name ?? node.menuName ?? node.title}` : node.name ?? node.menuName ?? node.title
    result.push({ id: node.id, label, level })
    if (node.children && node.children.length) {
      flattenTree(node.children, level + 1, result)
    }
  })
  return result
}

/** 去掉对象中的空字符串字段，避免把 "" 传给后端 */
export function cleanParams(params) {
  const result = {}
  Object.keys(params || {}).forEach((key) => {
    const value = params[key]
    if (value === '' || value === null || value === undefined) return
    if (Array.isArray(value) && value.length === 0) return
    result[key] = value
  })
  return result
}

const pad = (n) => String(n).padStart(2, '0')

/** 本地时间格式化：YYYY-MM-DD HH:mm:ss */
export function formatTime(value) {
  if (!value) return '-'
  if (typeof value === 'string') {
    // 兼容 "2024-01-01T12:00:00" 与 "2024-01-01 12:00:00"
    return value.replace('T', ' ').slice(0, 19)
  }
  if (!(value instanceof Date)) return String(value)
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(
    value.getHours()
  )}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

/** 将 "00:01:30" / 秒数 转为可读时长 */
export function formatDuration(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'string' && value.includes(':')) return value
  const total = Number(value)
  if (Number.isNaN(total)) return String(value)
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${pad(Math.floor(m / 60))}:${pad(m % 60)}:${pad(s)}`
}

/** 安全解析 JSON 字符串 */
export function safeParse(json, fallback) {
  if (!json) return fallback === undefined ? [] : fallback
  if (Array.isArray(json) || typeof json === 'object') return json
  try {
    const parsed = JSON.parse(json)
    return parsed === null ? (fallback === undefined ? [] : fallback) : parsed
  } catch (e) {
    return fallback === undefined ? [] : fallback
  }
}
