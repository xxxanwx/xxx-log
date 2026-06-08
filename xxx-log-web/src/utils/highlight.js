/**
 * 关键词高亮工具
 */

function escapeHtml(text) {
  if (text == null) {
    return ''
  }
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function escapeRegExp(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 对文本中的关键词进行 HTML 高亮
 * @param {string} text 原始文本
 * @param {string[]} keywords 关键词列表
 * @returns {string} 带 mark 标签的 HTML 字符串
 */
export function highlightKeywords(text, keywords) {
  if (!text) {
    return ''
  }
  const list = (keywords || []).map((k) => String(k).trim()).filter(Boolean)
  if (list.length === 0) {
    return escapeHtml(text)
  }
  const pattern = list.map(escapeRegExp).join('|')
  const regex = new RegExp(`(${pattern})`, 'gi')
  const escaped = escapeHtml(text)
  return escaped.replace(regex, '<mark class="kw-highlight">$1</mark>')
}

export { escapeHtml }
