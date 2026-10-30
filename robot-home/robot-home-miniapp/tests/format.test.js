'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const { imageOf } = require('../utils/format')
const { placeholderImage } = require('../utils/config')

test('uses the local neutral placeholder for random image services', () => {
  const randomImageUrls = [
    'https://picsum.photos/seed/unitree/200/200',
    'https://loremflickr.com/200/200/robot',
    'https://images.unsplash.com/photo-123?auto=format'
  ]

  for (const url of randomImageUrls) {
    assert.equal(imageOf(url), placeholderImage, url)
  }
})

test('keeps official image URLs and uses the placeholder for missing URLs', () => {
  const officialImage = 'https://cdn.example.com/robot.jpg'

  assert.equal(imageOf(officialImage), officialImage)
  assert.equal(imageOf(''), placeholderImage)
  assert.equal(imageOf(null), placeholderImage)
})
