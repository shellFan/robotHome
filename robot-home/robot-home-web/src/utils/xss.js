/**
 * 前端 XSS 兜底清洗：
 * 后端已对入库内容做过清洗，这里对富文本渲染再兜一层，
 * 移除脚本标签与事件属性，避免历史脏数据导致的注入风险。
 */
const SCRIPT_TAG = /<\s*(script|iframe|object|embed|link|style)\b[^>]*>[\s\S]*?<\s*\/\s*\1\s*>/gi
const SINGLE_TAG = /<\s*(script|iframe|object|embed|link|style)\b[^>]*\/?\s*>/gi
const EVENT_ATTR = /\son[a-zA-Z]+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi
const JS_URL = /\b(href|src|action)\s*=\s*(["']?)\s*javascript:/gi

export class XssUtil {
  static clean (html) {
    if (!html) return html
    return String(html)
      .replace(SCRIPT_TAG, '')
      .replace(SINGLE_TAG, '')
      .replace(EVENT_ATTR, '')
      .replace(JS_URL, (m, attr, quote) => attr + '=' + (quote || '') + '#')
  }
}

export default XssUtil
