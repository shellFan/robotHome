'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
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
