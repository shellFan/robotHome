const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '..')

function listWxml(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(directory, entry.name)
    return entry.isDirectory() ? listWxml(fullPath) : (entry.name.endsWith('.wxml') ? [fullPath] : [])
  })
}

test('WXML data bindings do not invoke JavaScript methods', () => {
  const violations = []
  const methodCall = /(?:[\w$]+|\])\s*\.\s*[A-Za-z_$][\w$]*\s*\(/g

  for (const file of listWxml(root)) {
    const source = fs.readFileSync(file, 'utf8')
    for (const match of source.matchAll(/\{\{([\s\S]*?)\}\}/g)) {
      if (methodCall.test(match[1])) {
        violations.push(`${path.relative(root, file)}: ${match[1].trim()}`)
        methodCall.lastIndex = 0
      }
    }
  }

  assert.deepEqual(violations, [], `Unsupported WXML method calls:\n${violations.join('\n')}`)
})
