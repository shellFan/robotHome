import test from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdir, mkdtemp, readFile, rm, symlink, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { buildPlan, validateManifest, applyBatch, rollbackBatch, parseArgs, loadManifest } from './import-content.mjs'

const importerPath = new URL('./import-content.mjs', import.meta.url)

const emptyState = () => ({ tool: 'robot-home-local-content-staging', schemaVersion: 1, records: {}, operations: [] })

function tutorialManifest(records) {
  return {
    schemaVersion: 1,
    records: records.map(({ sourceKey, title, content }) => ({
      sourceKey,
      payload: {
        title,
        summary: title,
        content: /参考/i.test(content) ? content : `${content}\n\n## 参考\n- https://example.org`,
        status: 0,
        viewCount: 0,
        likeCount: 0,
        favoriteCount: 0,
        commentCount: 0
      }
    }))
  }
}

test('validates a pending tutorial manifest and rejects published drafts', () => {
  const valid = validateManifest('tutorial', tutorialManifest([{ sourceKey: 'intro', title: '机器人入门', content: '## 章节\n\n正文\n\n## 参考\n- https://example.org' }]))
  assert.equal(valid.records.length, 1)
  const published = tutorialManifest([{ sourceKey: 'intro', title: '机器人入门', content: '## 章节\n\n正文\n\n## 参考\n- https://example.org' }])
  published.records[0].payload.status = 1
  assert.throws(() => validateManifest('tutorial', published), /status 0/)
})

test('rejects source keys that collide after whitespace normalization', () => {
  const manifest = tutorialManifest([
    { sourceKey: 'topic', title: '第一篇', content: '正文' },
    { sourceKey: ' topic ', title: '第二篇', content: '正文' }
  ])
  assert.throws(() => validateManifest('tutorial', manifest), /duplicate sourceKey: topic/)
})

test('treats prototype property names as ordinary staging keys', () => {
  const manifest = validateManifest('article', {
    schemaVersion: 1,
    records: ['__proto__', 'constructor', 'toString'].map(sourceKey => ({
      sourceKey,
      payload: { title: sourceKey, status: 0 }
    }))
  })
  const initial = emptyState()
  const plan = buildPlan('article', manifest.records, initial)
  assert.deepEqual(plan.map(item => item.action), ['INSERT_STAGING', 'INSERT_STAGING', 'INSERT_STAGING'])

  const applied = applyBatch(initial, plan, { batchId: 'prototype-keys' })
  const bucket = applied.records.article
  assert.equal(Object.keys(bucket).length, 3)
  for (const key of ['__proto__', 'constructor', 'toString']) assert.equal(Object.hasOwn(bucket, key), true)

  const restored = rollbackBatch(applied, 'prototype-keys')
  assert.deepEqual(restored.records, {})
})

test('plans inserts, idempotent skips, updates, and manual review without mutating state', () => {
  const input = tutorialManifest([
    { sourceKey: 'new', title: '新增', content: '正文' },
    { sourceKey: 'same', title: '相同', content: '同一版正文' },
    { sourceKey: 'changed', title: '更新', content: '新正文' },
    { sourceKey: 'review', title: '待审核', content: '未审正文' }
  ])
  input.records[3].reviewRequired = true
  const manifest = validateManifest('tutorial', input)
  const state = { ...emptyState(), records: { tutorial: {
    same: { payloadHash: manifest.records[1].payloadHash, payload: manifest.records[1].payload },
    changed: { payloadHash: 'old-hash', payload: { title: '旧版' } }
  } }, operations: [] }
  const plan = buildPlan('tutorial', manifest.records, state)
  assert.deepEqual(plan.map(item => item.action), ['INSERT_STAGING', 'SKIP_UNCHANGED', 'UPDATE_STAGING', 'REVIEW_REQUIRED'])
  assert.equal(state.operations.length, 0)
  assert.equal(state.records.tutorial.changed.payload.title, '旧版')
})

test('applies a batch atomically to staging and rolls it back from its audit record', () => {
  const records = validateManifest('tutorial', tutorialManifest([
    { sourceKey: 'a', title: 'A', content: '## 正文\n内容\n## 参考\n- https://example.org' },
    { sourceKey: 'b', title: 'B', content: '## 正文\n内容\n## 参考\n- https://example.org' }
  ])).records
  const initial = emptyState()
  const plan = buildPlan('tutorial', records, initial)
  const applied = applyBatch(initial, plan, { batchId: 'batch-1', now: '2026-10-30T00:00:00.000Z' })
  assert.equal(Object.keys(applied.records.tutorial).length, 2)
  assert.equal(applied.operations[0].batchId, 'batch-1')
  const restored = rollbackBatch(applied, 'batch-1')
  assert.deepEqual(restored.records, {})
  assert.ok(restored.operations.some(item => item.action === 'ROLLBACK'))
  const retried = rollbackBatch(restored, 'batch-1')
  assert.deepEqual(retried, restored)
})

test('refuses to roll back a batch after a later batch changed the same key', () => {
  const first = validateManifest('tutorial', tutorialManifest([{ sourceKey: 'a', title: 'A', content: '旧正文' }])).records
  const second = validateManifest('tutorial', tutorialManifest([{ sourceKey: 'a', title: 'A', content: '新正文' }])).records
  const initial = emptyState()
  const state1 = applyBatch(initial, buildPlan('tutorial', first, initial), { batchId: 'one', now: '2026-10-30T00:00:00.000Z' })
  const state2 = applyBatch(state1, buildPlan('tutorial', second, state1), { batchId: 'two', now: '2026-10-30T00:01:00.000Z' })
  assert.throws(() => rollbackBatch(state2, 'one'), /later operation/)
})

test('confines rollback state files to the report directory and rejects database options', () => {
  const root = path.join(tmpdir(), 'robot-home-import-test')
  assert.throws(() => parseArgs([
    '--apply', '--rollback', 'batch-1',
    '--state-file', path.join(root, 'outside', 'state.json'),
    '--report-dir', path.join(root, 'reports')
  ]), /inside --report-dir/)
  assert.throws(() => parseArgs([
    '--apply', '--content-type', 'tutorial', '--source-file', 'manifest.json',
    '--report-dir', path.join(root, 'reports'), '--jdbc-url', 'jdbc:mysql://host/db'
  ]), /unknown option/)
  assert.throws(() => parseArgs([
    '--apply', '--content-type', 'tutorial', '--source-file', 'manifest.json',
    '--report-dir', path.join(root, 'reports'), '--state-file', path.join(root, 'reports')
  ]), /inside --report-dir/)
})

test('rejects state files redirected outside the report directory by a junction', async t => {
  const root = await mkdtemp(path.join(tmpdir(), 'robot-home-import-junction-'))
  try {
    const reportDir = path.join(root, 'reports')
    const outsideDir = path.join(root, 'outside')
    await mkdir(reportDir)
    await mkdir(outsideDir)
    try {
      await symlink(outsideDir, path.join(reportDir, 'escape'), 'junction')
    } catch (error) {
      if (['EPERM', 'EACCES', 'ENOSYS', 'EINVAL'].includes(error.code)) {
        t.skip(`directory junction unavailable: ${error.code}`)
        return
      }
      throw error
    }
    const sourceFile = path.join(root, 'tutorials.json')
    await writeFile(sourceFile, JSON.stringify(tutorialManifest([
      { sourceKey: 'safe', title: '安全', content: '正文' }
    ])), 'utf8')
    const run = spawnSync(process.execPath, [fileURLToPath(importerPath),
      '--content-type', 'tutorial', '--source-file', sourceFile,
      '--report-dir', reportDir, '--state-file', path.join(reportDir, 'escape', 'state.json'), '--apply'
    ], { encoding: 'utf8' })
    assert.notEqual(run.status, 0, run.stdout)
    assert.match(run.stderr, /inside --report-dir/)
    await assert.rejects(readFile(path.join(outsideDir, 'state.json'), 'utf8'), { code: 'ENOENT' })
  } finally {
    await rm(root, { recursive: true, force: true })
  }
})

test('CLI dry-run leaves staging untouched; apply batches idempotently and rollback restores state', async () => {
  const root = await mkdtemp(path.join(tmpdir(), 'robot-home-content-import-'))
  try {
    const reportDir = path.join(root, 'reports')
    const sourceFile = path.join(root, 'tutorials.json')
    const manifest = tutorialManifest([
      { sourceKey: 'one', title: '教程一', content: '## 章节一\n正文' },
      { sourceKey: 'two', title: '教程二', content: '## 章节一\n正文' }
    ])
    await writeFile(sourceFile, JSON.stringify(manifest), 'utf8')
    const common = ['--content-type', 'tutorial', '--source-file', sourceFile, '--report-dir', reportDir, '--batch-size', '2']
    const run = args => spawnSync(process.execPath, [fileURLToPath(importerPath), ...args], { encoding: 'utf8' })

    const dryRun = run([...common, '--dry-run'])
    assert.equal(dryRun.status, 0, dryRun.stderr)
    assert.match(dryRun.stdout, /DRY-RUN tutorial INSERT_STAGING=2/)
    await assert.rejects(readFile(path.join(reportDir, 'staging-state.json'), 'utf8'), { code: 'ENOENT' })

    const apply = run([...common, '--apply'])
    assert.equal(apply.status, 0, apply.stderr)
    const stateFile = path.join(reportDir, 'staging-state.json')
    const stateAfterApply = JSON.parse(await readFile(stateFile, 'utf8'))
    assert.equal(stateAfterApply.operations.length, 2)
    const applyReportFile = apply.stdout.split(/\r?\n/).find(line => line.startsWith('REPORT ')).slice(7)
    const applyReport = JSON.parse(await readFile(applyReportFile, 'utf8'))
    assert.equal(applyReport.status, 'APPLIED')
    assert.equal(applyReport.appliedBatches, 1)

    const repeat = run([...common, '--apply'])
    assert.equal(repeat.status, 0, repeat.stderr)
    const stateAfterRepeat = JSON.parse(await readFile(stateFile, 'utf8'))
    assert.equal(stateAfterRepeat.operations.length, 2)
    assert.match(repeat.stdout, /SKIP_UNCHANGED=2/)
    const repeatReportFile = repeat.stdout.split(/\r?\n/).find(line => line.startsWith('REPORT ')).slice(7)
    const repeatReport = JSON.parse(await readFile(repeatReportFile, 'utf8'))
    assert.equal(repeatReport.status, 'NO_CHANGES')
    assert.equal(repeatReport.appliedBatches, 0)

    const batchId = stateAfterRepeat.operations[0].batchId
    const rollback = run(['--apply', '--rollback', batchId, '--state-file', stateFile, '--report-dir', reportDir])
    assert.equal(rollback.status, 0, rollback.stderr)
    const stateAfterRollback = JSON.parse(await readFile(stateFile, 'utf8'))
    assert.deepEqual(stateAfterRollback.records, {})
    assert.equal(stateAfterRollback.operations.filter(item => item.action === 'ROLLBACK').length, 2)
    const rollbackReportFile = path.join(reportDir, `rollback-${batchId}.json`)
    assert.equal(JSON.parse(await readFile(rollbackReportFile, 'utf8')).result, 'ROLLED_BACK')

    const rollbackRetry = run(['--apply', '--rollback', batchId, '--state-file', stateFile, '--report-dir', reportDir])
    assert.equal(rollbackRetry.status, 0, rollbackRetry.stderr)
    const stateAfterRollbackRetry = JSON.parse(await readFile(stateFile, 'utf8'))
    assert.deepEqual(stateAfterRollbackRetry, stateAfterRollback)
    assert.equal(JSON.parse(await readFile(rollbackReportFile, 'utf8')).result, 'ALREADY_ROLLED_BACK')
  } finally {
    await rm(root, { recursive: true, force: true })
  }
})

test('does not report applied batches or create staging state for manual-review records', async () => {
  const root = await mkdtemp(path.join(tmpdir(), 'robot-home-review-only-'))
  try {
    const reportDir = path.join(root, 'reports')
    const sourceFile = path.join(root, 'tutorials.json')
    const manifest = tutorialManifest([{ sourceKey: 'review', title: '待复核', content: '正文' }])
    manifest.records[0].reviewRequired = true
    manifest.records[0].reviewReason = '需要人工审核'
    await writeFile(sourceFile, JSON.stringify(manifest), 'utf8')
    const run = spawnSync(process.execPath, [fileURLToPath(importerPath),
      '--content-type', 'tutorial', '--source-file', sourceFile,
      '--report-dir', reportDir, '--apply'
    ], { encoding: 'utf8' })
    assert.equal(run.status, 0, run.stderr)
    assert.match(run.stdout, /REVIEW_REQUIRED=1/)
    const reportFile = run.stdout.split(/\r?\n/).find(line => line.startsWith('REPORT ')).slice(7)
    const report = JSON.parse(await readFile(reportFile, 'utf8'))
    assert.equal(report.appliedBatches, 0)
    assert.equal(report.status, 'NO_CHANGES')
    await assert.rejects(readFile(path.join(reportDir, 'staging-state.json'), 'utf8'), { code: 'ENOENT' })
  } finally {
    await rm(root, { recursive: true, force: true })
  }
})

test('loads tutorial Markdown directories as unpublished, source-keyed records', async () => {
  const root = await mkdtemp(path.join(tmpdir(), 'robot-home-tutorial-import-'))
  try {
    await writeFile(path.join(root, '01-basics.md'), '# 机器人基础分类\n\n**内容状态：原创草稿，待技术与后台审核；未发布。**\n\n## 分类\n\n从任务和环境判断。\n\n## 参考\n- https://example.org/robots', 'utf8')
    const manifest = await loadManifest('tutorial', root)
    const validated = validateManifest('tutorial', manifest)
    assert.equal(validated.records.length, 1)
    assert.equal(validated.records[0].sourceKey, '01-basics')
    assert.equal(validated.records[0].payload.title, '机器人基础分类')
    assert.equal(validated.records[0].payload.summary, '从任务和环境判断。')
    assert.equal(validated.records[0].payload.status, 0)
    assert.match(validated.records[0].payload.content, /https:\/\/example\.org\/robots/)
  } finally {
    await rm(root, { recursive: true, force: true })
  }
})
