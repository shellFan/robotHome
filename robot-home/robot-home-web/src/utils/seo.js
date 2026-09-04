/**
 * SEO 工具：SPA 场景下动态设置 title / description / keywords
 * 说明：第一版为 Vue SPA，通过 JS 注入 meta 满足基础 SEO；
 *      后续升级 SSR / Nuxt 时可直接复用本模块的 meta 生成逻辑。
 */
export const DEFAULT_META = {
  title: '机器人之家 - 机器人产品库 · 品牌库 · 企业库 · 资讯 · 参数对比',
  description: '机器人之家是机器人行业的垂直平台，提供机器人产品数据库、品牌库、企业库、行业资讯、视频、参数对比、排行榜、社区与询价采购服务。',
  keywords: '机器人,人形机器人,机器狗,四足机器人,服务机器人,工业机器人,扫地机器人,机器人品牌,机器人企业,机器人参数对比,机器人排行榜'
}

function upsertMeta (attr, key, content) {
  if (!content) return
  let el = document.head.querySelector('meta[' + attr + '="' + key + '"]')
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

export function setPageMeta (meta) {
  const item = meta || {}
  if (item.title) {
    document.title = item.title
    upsertMeta('property', 'og:title', item.title)
  }
  if (item.description) {
    upsertMeta('name', 'description', item.description)
    upsertMeta('property', 'og:description', item.description)
  }
  if (item.keywords) {
    upsertMeta('name', 'keywords', item.keywords)
  }
}
