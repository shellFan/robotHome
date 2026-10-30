import { test } from 'node:test'
import assert from 'node:assert/strict'
import { imageSource, PLACEHOLDER_IMAGE } from '../src/utils/image.js'

test('uses the local neutral placeholder for missing and random image URLs', () => {
  for (const url of [
    '',
    null,
    '   ',
    'https://picsum.photos/seed/brand/200/200',
    'https://loremflickr.com/200/200/robot',
    'https://images.unsplash.com/photo-123?auto=format'
  ]) {
    assert.equal(imageSource(url), PLACEHOLDER_IMAGE, String(url))
  }
})

test('keeps non-random official image URLs unchanged', () => {
  const officialImage = 'https://cdn.example.com/products/robot.jpg'
  assert.equal(imageSource(officialImage), officialImage)
})
