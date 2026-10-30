'use strict'

const DIRECT_MEDIA_PATH = /\.(?:mp4|webm|m3u8)$/i
const ABSOLUTE_HTTP_URL = /^(https?):\/\/([^/?#]+)(\/[^?#]*)?(?:\?[^#]*)?(?:#.*)?$/i
const DOMAIN_NAME = /^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*\.?$/i

function none() {
  return { kind: 'none', url: '' }
}

function isValidIpv4(value) {
  const parts = value.split('.')
  return parts.length === 4 && parts.every((part) => {
    if (!/^\d{1,3}$/.test(part) || (part.length > 1 && part[0] === '0')) return false
    return Number(part) <= 255
  })
}

function isValidIpv6(value) {
  if (!value || value.indexOf('::') !== value.lastIndexOf('::')) return false

  let groups = value
  let ipv4Groups = 0
  if (value.indexOf('.') !== -1) {
    const lastColon = value.lastIndexOf(':')
    if (lastColon === -1 || !isValidIpv4(value.slice(lastColon + 1))) return false
    groups = value.slice(0, lastColon)
    ipv4Groups = 2
  }

  const compressed = groups.indexOf('::') !== -1
  const halves = groups.split('::')
  const left = halves[0] ? halves[0].split(':') : []
  const right = compressed && halves[1] ? halves[1].split(':') : []
  const allGroups = left.concat(right)
  if (!allGroups.every((group) => /^[a-f0-9]{1,4}$/i.test(group))) return false

  const count = allGroups.length + ipv4Groups
  return compressed ? count < 8 : count === 8
}

function isValidAuthority(authority) {
  if (!authority || /[@%\\\s\u0000-\u001f\u007f]/.test(authority)) return false

  let hostname
  let port = ''
  if (authority[0] === '[') {
    const match = /^(\[([a-f0-9:.]+)\])(?::(\d+))?$/i.exec(authority)
    if (!match || !isValidIpv6(match[2])) return false
    hostname = match[2]
    port = match[3] || ''
  } else {
    const match = /^([^:]+)(?::(\d+))?$/.exec(authority)
    if (!match) return false
    hostname = match[1]
    port = match[2] || ''
    if (/^\d+(?:\.\d+){3}$/.test(hostname)) {
      if (!isValidIpv4(hostname)) return false
    } else if (!DOMAIN_NAME.test(hostname) || hostname.length > 253) {
      return false
    }
  }

  return !port || Number(port) <= 65535
}

function classifyVideoUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return none()

  const url = value.trim()
  if (/[\s\u0000-\u001f\u007f\\]/.test(url)) return none()

  const match = ABSOLUTE_HTTP_URL.exec(url)
  if (!match || !isValidAuthority(match[2])) return none()

  return {
    kind: DIRECT_MEDIA_PATH.test(match[3] || '') ? 'media' : 'external',
    url
  }
}

function resolveVideoSource(video) {
  if (!video || typeof video !== 'object') return none()
  return classifyVideoUrl(video.playUrl || video.url || video.videoUrl || '')
}

module.exports = { classifyVideoUrl, resolveVideoSource }
