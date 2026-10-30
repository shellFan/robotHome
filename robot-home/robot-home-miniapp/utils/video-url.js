'use strict'

const DIRECT_MEDIA_PATH = /\.(?:mp4|webm|m3u8)$/i

function none() {
  return { kind: 'none', url: '' }
}

function classifyVideoUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return none()

  try {
    const parsed = new URL(value.trim())
    if (
      (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') ||
      !parsed.hostname ||
      parsed.username ||
      parsed.password
    ) {
      return none()
    }

    return {
      kind: DIRECT_MEDIA_PATH.test(parsed.pathname) ? 'media' : 'external',
      url: parsed.href
    }
  } catch (error) {
    return none()
  }
}

function resolveVideoSource(video) {
  if (!video || typeof video !== 'object') return none()
  return classifyVideoUrl(video.playUrl || video.url || video.videoUrl || '')
}

module.exports = { classifyVideoUrl, resolveVideoSource }
