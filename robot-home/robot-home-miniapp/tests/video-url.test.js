'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { classifyVideoUrl, resolveVideoSource } = require('../utils/video-url')

test('classifies direct media by pathname and ignores query strings and fragments', () => {
  assert.deepEqual(classifyVideoUrl('https://cdn.example/video.mp4?sig=x'), {
    kind: 'media',
    url: 'https://cdn.example/video.mp4?sig=x'
  })
  assert.equal(classifyVideoUrl('https://cdn.example/clip.WEBM').kind, 'media')
  assert.equal(classifyVideoUrl('https://cdn.example/live.m3u8#t=4').kind, 'media')
  assert.equal(classifyVideoUrl('https://example.com/watch?file=clip.mp4').kind, 'external')
})

test('classifies platform pages as external', () => {
  assert.deepEqual(classifyVideoUrl('https://www.youtube.com/watch?v=abc'), {
    kind: 'external',
    url: 'https://www.youtube.com/watch?v=abc'
  })
})

test('classifies URLs in a MiniApp runtime without a global URL constructor', () => {
  const helperPath = path.resolve(__dirname, '../utils/video-url.js')
  const module = { exports: {} }
  vm.runInNewContext(fs.readFileSync(helperPath, 'utf8'), { module, exports: module.exports })

  const result = module.exports.classifyVideoUrl('https://cdn.example/clip.mp4?sig=x')
  assert.equal(result.kind, 'media')
  assert.equal(result.url, 'https://cdn.example/clip.mp4?sig=x')
  assert.equal(module.exports.classifyVideoUrl('https://user@cdn.example/clip.mp4').kind, 'none')
})

test('rejects missing, malformed, non-HTTP, and credential-bearing URLs', () => {
  for (const value of [
    '',
    '   ',
    'not a URL',
    'javascript:alert(1)',
    'ftp://cdn.example/video.mp4',
    '//cdn.example/video.mp4',
    'https://user:password@cdn.example/video.mp4'
  ]) {
    assert.deepEqual(classifyVideoUrl(value), { kind: 'none', url: '' }, value)
  }
})

test('resolves existing URL fields in playUrl, url, videoUrl order', () => {
  assert.equal(resolveVideoSource({
    playUrl: 'https://cdn.example/primary.mp4',
    url: 'https://platform.example/watch'
  }).kind, 'media')
  assert.equal(resolveVideoSource({
    url: 'https://platform.example/watch',
    videoUrl: 'https://cdn.example/fallback.mp4'
  }).kind, 'external')
  assert.deepEqual(resolveVideoSource(null), { kind: 'none', url: '' })
})

function createPage(video, clipboardBehavior) {
  let definition
  let clipboardOptions
  const toasts = []
  const pagePath = path.resolve(__dirname, '../pages/videos/detail.js')
  const source = fs.readFileSync(pagePath, 'utf8')
  const wx = {
    setNavigationBarTitle() {},
    showToast(options) { toasts.push(options) },
    setClipboardData(options) {
      clipboardOptions = options
      if (clipboardBehavior === 'success') options.success()
      if (clipboardBehavior === 'fail') options.fail(new Error('clipboard denied'))
    }
  }
  const context = vm.createContext({
    Page(value) { definition = value },
    wx,
    require(specifier) {
      if (specifier === '../../api/index') {
        return {
          videoApi: { detail: async () => video },
          likeApi: { toggle: async () => ({ liked: true }) },
          favoriteApi: { toggle: async () => ({ favorited: true }) }
        }
      }
      if (specifier === '../../utils/format') {
        return {
          formatCount: String,
          formatDuration: String,
          imageOf: (value) => value || '/placeholder.png'
        }
      }
      if (specifier === '../../utils/video-url') return { resolveVideoSource }
      if (specifier === '../../utils/request') return { ensureLogin: () => true }
      throw new Error('Unexpected module: ' + specifier)
    }
  })
  vm.runInContext(source, context, { filename: pagePath })
  const page = {
    ...definition,
    data: JSON.parse(JSON.stringify(definition.data)),
    id: 'video-1',
    setData(patch) { this.data = Object.assign({}, this.data, patch) }
  }
  return { page, toasts, getClipboardOptions: () => clipboardOptions }
}

test('external platform videos never become player sources and copy after a tap', async () => {
  const url = 'https://www.youtube.com/watch?v=abc'
  const { page, toasts, getClipboardOptions } = createPage({ title: '外部视频', url }, 'success')
  await page.load()

  assert.equal(page.data.video.playbackKind, 'external')
  assert.equal(page.data.video.showPlayer, false)
  assert.equal(page.data.video.playUrl, '')
  assert.equal(page.data.video.externalUrl, url)

  page.onCopyVideoUrl()
  assert.equal(getClipboardOptions().data, url)
  assert.equal(toasts.at(-1).title, '已复制，请前往来源平台播放')
})

test('direct media errors expose a copy fallback and clipboard failures are reported', async () => {
  const url = 'https://cdn.example/clip.mp4?sig=x'
  const { page, toasts, getClipboardOptions } = createPage({ title: '直链视频', playUrl: url }, 'fail')
  await page.load()

  assert.equal(page.data.video.playbackKind, 'media')
  assert.equal(page.data.video.playUrl, url)
  page.onVideoError()
  assert.equal(page.data.video.showPlayer, false)
  assert.equal(page.data.video.showCopyOnFailure, true)

  page.onCopyVideoUrl()
  assert.equal(getClipboardOptions().data, url)
  assert.equal(toasts.at(-1).title, '复制失败，请稍后重试')
})

test('MiniApp template binds the native player only to the classified playUrl', () => {
  const template = fs.readFileSync(path.resolve(__dirname, '../pages/videos/detail.wxml'), 'utf8')
  assert.match(template, /wx:if="\{\{video\.showPlayer\}\}"/)
  assert.match(template, /src="\{\{video\.playUrl\}\}"/)
  assert.match(template, /binderror="onVideoError"/)
  assert.match(template, /bindtap="onCopyVideoUrl"/)
  assert.doesNotMatch(template, /src="\{\{video\.url\}\}"/)
})
