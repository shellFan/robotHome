import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const viewRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../src/views')
const detailView = fs.readFileSync(path.join(viewRoot, 'VideoDetail.vue'), 'utf8')
const robotVideosView = fs.readFileSync(path.join(viewRoot, 'RobotVideos.vue'), 'utf8')

test('video detail only sends classified media URLs to the native player', () => {
  assert.match(detailView, /videoSource\.kind === 'media' && !playbackFailed/)
  assert.match(detailView, /@error="onVideoError"/)
  assert.match(detailView, /videoSource\.kind === 'external' \|\| playbackFailed/)
  assert.match(detailView, /target="_blank"[\s\S]*rel="noopener noreferrer"/)
  assert.doesNotMatch(detailView, /<video v-if="detail\.url"/)
})

test('robot video dialog keeps platform pages outside the native player', () => {
  assert.match(robotVideosView, /player\.kind === 'media' && !player\.error/)
  assert.match(robotVideosView, /@error="onPlayerError"/)
  assert.match(robotVideosView, /player\.kind === 'external'/)
  assert.match(robotVideosView, /target="_blank"[\s\S]*rel="noopener noreferrer"/)
  assert.doesNotMatch(robotVideosView, /v-if="player\.url"/)
})
