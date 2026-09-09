/**
 * 前端 XSS 净化工具 — 基于 DOMPurify
 * 后端入库前已做 HTML 白名单清洗，前端渲染前再兜一层防护。
 * 正则方案无法防御所有 XSS 向量，必须使用 DOMPurify。
 */
import DOMPurify from 'dompurify'

/** 允许的富文本标签白名单 */
const ALLOWED_TAGS = [
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'p', 'br', 'hr', 'blockquote', 'pre', 'code',
  'ul', 'ol', 'li', 'dl', 'dt', 'dd',
  'a', 'img',
  'strong', 'em', 'b', 'i', 'u', 's', 'del', 'ins', 'mark', 'sub', 'sup',
  'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td', 'caption', 'colgroup', 'col',
  'figure', 'figcaption', 'details', 'summary', 'abbr',
  'div', 'span', 'section', 'article', 'aside', 'header', 'footer', 'nav', 'main'
]

/** 允许的属性白名单 */
const ALLOWED_ATTR = [
  'href', 'src', 'alt', 'title', 'class', 'id', 'name',
  'target', 'rel', 'download',
  'width', 'height', 'colspan', 'rowspan', 'scope',
  'start', 'type', 'reversed', 'loading', 'decoding'
]

/** 禁止的标签 */
const FORBID_TAGS = ['script', 'iframe', 'object', 'embed', 'form', 'input', 'textarea', 'select', 'button', 'meta', 'link', 'style', 'base', 'applet', 'frame', 'frameset']

/** 禁止的属性 */
const FORBID_ATTR = ['onabort', 'onblur', 'onchange', 'onclick', 'ondblclick', 'onerror', 'onfocus', 'onkeydown', 'onkeypress', 'onkeyup', 'onload', 'onmousedown', 'onmousemove', 'onmouseout', 'onmouseover', 'onmouseup', 'onreset', 'onresize', 'onscroll', 'onselect', 'onsubmit', 'onunload', 'formaction', 'xlink:href']

/** 初始化 DOMPurify 配置 */
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A') {
    node.setAttribute('rel', 'noopener noreferrer')
    const href = node.getAttribute('href') || ''
    if (href.startsWith('http://') || href.startsWith('https://')) {
      node.setAttribute('target', '_blank')
    }
  }
  if (node.tagName === 'IMG') {
    if (!node.getAttribute('loading')) {
      node.setAttribute('loading', 'lazy')
    }
  }
})

export class XssUtil {
  static clean (html) {
    if (!html) return html
    return DOMPurify.sanitize(String(html), {
      ALLOWED_TAGS,
      ALLOWED_ATTR,
      FORBID_TAGS,
      FORBID_ATTR,
      ALLOW_DATA_ATTR: false,
      RETURN_DOM: false,
      RETURN_DOM_FRAGMENT: false,
      RETURN_DOM_IMPORT: false
    })
  }
}

export default XssUtil