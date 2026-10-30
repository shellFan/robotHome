#!/usr/bin/env node

import { createHash, randomUUID } from 'node:crypto'
import { mkdir, readFile, readdir, realpath, rename, stat, unlink, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const TOOL_NAME = 'robot-home-local-content-staging'
const CONTENT_TYPES = new Set(['article', 'product', 'video', 'tutorial', 'community'])
const MAX_BATCH_SIZE = 500

function stableJson(value) {
  if (Array.isArray(value)) return `[${value.map(stableJson).join(',')}]`
  if (value && typeof value === 'object') {
    return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${stableJson(value[key])}`).join(',')}}`
  }
  return JSON.stringify(value)
}

function sha256(value) {
  return createHash('sha256').update(value).digest('hex')
}

function markdownSummary(content, title) {
  const lines = content.split(/\r?\n/)
  let collecting = false
  for (const line of lines) {
    const value = line.trim()
    if (!collecting) {
      if (/^#{1,6}\s+/.test(value)) collecting = true
      continue
    }
    if (!value || /^#{1,6}\s+/.test(value) || /^[-*]\s+/.test(value) || /^\d+[.)]\s+/.test(value) || /^\*\*内容状态[:：]/.test(value)) continue
    return value.slice(0, 280)
  }
  return title
}

async function markdownFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  return entries.filter(entry => entry.isFile() && entry.name.toLowerCase().endsWith('.md'))
    .map(entry => entry.name).sort((a, b) => a.localeCompare(b, 'en'))
}

export async function loadManifest(contentType, sourceFile) {
  const sourcePath = path.resolve(sourceFile)
  const sourceStat = await stat(sourcePath)
  if (sourceStat.isFile()) return JSON.parse(await readFile(sourcePath, 'utf8'))
  if (!sourceStat.isDirectory() || contentType !== 'tutorial') {
    throw new Error('source file must be a JSON manifest; only tutorial content also accepts a directory of Markdown files')
  }
  const files = await markdownFiles(sourcePath)
  if (files.length === 0) throw new Error('tutorial source directory contains no Markdown files')
  const records = []
  for (const fileName of files) {
    const content = await readFile(path.join(sourcePath, fileName), 'utf8')
    const titleMatch = content.match(/^#\s+(.+)$/m)
    if (!titleMatch) throw new Error(`${fileName} must start with a Markdown H1 title`)
    const title = titleMatch[1].trim()
    records.push({
      sourceKey: path.basename(fileName, path.extname(fileName)),
      payload: {
        title,
        summary: markdownSummary(content, title),
        content,
        status: 0,
        viewCount: 0,
        likeCount: 0,
        favoriteCount: 0,
        commentCount: 0
      }
    })
  }
  return { schemaVersion: 1, records }
}

export async function hashSourceFile(sourceFile, contentType) {
  const sourcePath = path.resolve(sourceFile)
  const sourceStat = await stat(sourcePath)
  if (sourceStat.isFile()) return sha256(await readFile(sourcePath))
  if (!sourceStat.isDirectory() || contentType !== 'tutorial') throw new Error('unsupported source directory')
  const digest = createHash('sha256')
  const files = await markdownFiles(sourcePath)
  for (const fileName of files) {
    digest.update(fileName, 'utf8')
    digest.update('\0', 'utf8')
    digest.update(await readFile(path.join(sourcePath, fileName)))
    digest.update('\0', 'utf8')
  }
  return digest.digest('hex')
}

function assertObject(value, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${label} must be a JSON object`)
  }
}

function nonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0
}

function ownValue(object, key) {
  return Object.hasOwn(object, key) ? object[key] : null
}

function setOwnValue(object, key, value) {
  Object.defineProperty(object, key, {
    configurable: true,
    enumerable: true,
    writable: true,
    value
  })
}

export function validateManifest(contentType, manifest) {
  if (!CONTENT_TYPES.has(contentType)) {
    throw new Error(`unsupported content type: ${contentType}`)
  }
  assertObject(manifest, 'manifest')
  if (manifest.schemaVersion !== 1) throw new Error('manifest schemaVersion must be 1')
  if (!Array.isArray(manifest.records) || manifest.records.length === 0) {
    throw new Error('manifest records must be a non-empty array')
  }

  const seen = new Set()
  const records = manifest.records.map((item, index) => {
    assertObject(item, `records[${index}]`)
    if (!nonEmptyString(item.sourceKey) || item.sourceKey.length > 191) {
      throw new Error(`records[${index}].sourceKey must be 1-191 characters`)
    }
    const sourceKey = item.sourceKey.trim()
    if (seen.has(sourceKey)) throw new Error(`duplicate sourceKey: ${sourceKey}`)
    seen.add(sourceKey)
    assertObject(item.payload, `records[${index}].payload`)

    const payload = structuredClone(item.payload)
    if (!nonEmptyString(payload.title || payload.name)) {
      throw new Error(`records[${index}] needs a non-empty title or name`)
    }
    if (payload.status !== 0) throw new Error(`records[${index}] must have status 0 (pending review)`)

    if (contentType === 'tutorial') {
      if (!nonEmptyString(payload.content)) throw new Error(`records[${index}] tutorial content is required`)
      if (!/参考/i.test(payload.content) || !/https:\/\//i.test(payload.content)) {
        throw new Error(`records[${index}] tutorial must include references with an HTTPS source`)
      }
    }
    if (contentType === 'video') {
      if (!/^https:\/\//i.test(payload.url || '')) throw new Error(`records[${index}] video URL must use HTTPS`)
      if (!/^https:\/\//i.test(payload.officialSource || '')) throw new Error(`records[${index}] officialSource must use HTTPS`)
      if (payload.playbackVerified !== true && item.reviewRequired !== true) {
        throw new Error(`records[${index}] unverified playback must be marked reviewRequired`)
      }
    }
    if (contentType === 'community') {
      if (!Number.isSafeInteger(payload.userId) && item.reviewRequired !== true) {
        throw new Error(`records[${index}] community drafts need an authorized userId or reviewRequired`)
      }
      for (const field of ['viewCount', 'likeCount', 'favoriteCount', 'commentCount']) {
        if (payload[field] !== undefined && payload[field] !== 0) {
          throw new Error(`records[${index}] community ${field} must be zero`)
        }
      }
    }
    if (contentType === 'product' && !/^https:\/\//i.test(payload.officialUrl || '')) {
      throw new Error(`records[${index}] product officialUrl must use HTTPS`)
    }
    for (const field of ['viewCount', 'likeCount', 'favoriteCount', 'commentCount']) {
      if (['video', 'tutorial'].includes(contentType) && payload[field] !== undefined && payload[field] !== 0) {
        throw new Error(`records[${index}] pending ${contentType} ${field} must be zero`)
      }
    }

    return {
      sourceKey,
      payload,
      payloadHash: sha256(stableJson(payload)),
      reviewRequired: item.reviewRequired === true,
      reviewReason: nonEmptyString(item.reviewReason) ? item.reviewReason.trim() : null
    }
  })
  return { schemaVersion: 1, contentType, records, manifestHash: sha256(stableJson(manifest)) }
}

function emptyState() {
  return { tool: TOOL_NAME, schemaVersion: 1, records: {}, operations: [] }
}

function validateState(state) {
  if (state === undefined || state === null) return emptyState()
  assertObject(state, 'staging state')
  if (state.tool !== TOOL_NAME || state.schemaVersion !== 1 || !state.records || Array.isArray(state.records) || !Array.isArray(state.operations)) {
    throw new Error('staging state is not a compatible importer state file')
  }
  return state
}

function assertInsideReportDir(reportDirInput, stateFileInput) {
  const reportDir = path.resolve(reportDirInput)
  const stateFile = path.resolve(stateFileInput)
  const relative = path.relative(reportDir, stateFile)
  if (!relative || relative === '..' || relative.startsWith(`..${path.sep}`) || path.isAbsolute(relative)) {
    throw new Error('--state-file must be inside --report-dir')
  }
}

async function resolvePathThroughExistingParents(targetPath) {
  let current = path.resolve(targetPath)
  const remaining = []
  while (true) {
    try {
      const resolvedParent = await realpath(current)
      return path.join(resolvedParent, ...remaining)
    } catch (error) {
      if (error.code !== 'ENOENT' && error.code !== 'ENOTDIR') throw error
      const parent = path.dirname(current)
      if (parent === current) throw error
      remaining.unshift(path.basename(current))
      current = parent
    }
  }
}

async function assertRealPathInsideReportDir(reportDirInput, stateFileInput) {
  const reportDir = await realpath(reportDirInput)
  const stateFile = await resolvePathThroughExistingParents(stateFileInput)
  const relative = path.relative(reportDir, stateFile)
  if (!relative || relative === '..' || relative.startsWith(`..${path.sep}`) || path.isAbsolute(relative)) {
    throw new Error('--state-file must be inside --report-dir')
  }
}

export function buildPlan(contentType, records, stateInput) {
  if (!CONTENT_TYPES.has(contentType)) throw new Error(`unsupported content type: ${contentType}`)
  const state = validateState(stateInput)
  const existing = ownValue(state.records, contentType) || {}
  return records.map(record => {
    const before = ownValue(existing, record.sourceKey)
    let action = 'INSERT_STAGING'
    if (record.reviewRequired) action = 'REVIEW_REQUIRED'
    else if (before && before.payloadHash === record.payloadHash) action = 'SKIP_UNCHANGED'
    else if (before) action = 'UPDATE_STAGING'
    return {
      contentType,
      sourceKey: record.sourceKey,
      action,
      payloadHash: record.payloadHash,
      payload: record.payload,
      reviewReason: record.reviewReason,
      before: before ? structuredClone(before) : null
    }
  })
}

export function applyBatch(stateInput, plan, { batchId, now = new Date().toISOString() }) {
  const state = validateState(stateInput)
  if (!nonEmptyString(batchId)) throw new Error('batchId is required')
  const next = structuredClone(state)
  let index = 0
  for (const item of plan) {
    if (!['INSERT_STAGING', 'UPDATE_STAGING'].includes(item.action)) continue
    let bucket = ownValue(next.records, item.contentType)
    if (!bucket) {
      bucket = {}
      setOwnValue(next.records, item.contentType, bucket)
    }
    const current = ownValue(bucket, item.sourceKey)
    if ((current?.payloadHash || null) !== (item.before?.payloadHash || null)) {
      throw new Error(`staging state changed while planning: ${item.contentType}/${item.sourceKey}`)
    }
    const operationId = `${batchId}:${++index}`
    const value = {
      payload: structuredClone(item.payload),
      payloadHash: item.payloadHash,
      lastBatchId: batchId,
      lastOperationId: operationId
    }
    setOwnValue(bucket, item.sourceKey, value)
    next.operations.push({
      operationId,
      batchId,
      contentType: item.contentType,
      sourceKey: item.sourceKey,
      action: item.action,
      before: current ? structuredClone(current) : null,
      afterHash: item.payloadHash,
      createdAt: now
    })
  }
  return next
}

export function rollbackBatch(stateInput, batchId, now = new Date().toISOString()) {
  const state = validateState(stateInput)
  const batchOperations = state.operations.filter(item => item.batchId === batchId && item.action !== 'ROLLBACK')
  if (batchOperations.length === 0) throw new Error(`batch not found: ${batchId}`)
  const rolledBackIds = new Set(state.operations.filter(item => item.action === 'ROLLBACK').map(item => item.rollbackOf))
  const pendingOperations = batchOperations.filter(item => !rolledBackIds.has(item.operationId))
  if (pendingOperations.length === 0) return structuredClone(state)
  const next = structuredClone(state)
  for (const operation of [...pendingOperations].reverse()) {
    const bucket = ownValue(next.records, operation.contentType) || {}
    const current = ownValue(bucket, operation.sourceKey)
    if (!current || current.lastOperationId !== operation.operationId) {
      throw new Error(`cannot roll back ${batchId}: later operation exists for ${operation.contentType}/${operation.sourceKey}`)
    }
    if (operation.before) setOwnValue(bucket, operation.sourceKey, structuredClone(operation.before))
    else delete bucket[operation.sourceKey]
    next.operations.push({
      operationId: `${operation.operationId}:rollback`,
      batchId: `rollback:${batchId}`,
      rollbackOf: operation.operationId,
      contentType: operation.contentType,
      sourceKey: operation.sourceKey,
      action: 'ROLLBACK',
      createdAt: now
    })
    if (Object.keys(bucket).length === 0) delete next.records[operation.contentType]
  }
  return next
}

export function parseArgs(argv) {
  const options = { batchSize: 20 }
  const flags = new Set()
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (['--dry-run', '--apply'].includes(arg)) {
      if (flags.has(arg)) throw new Error(`duplicate flag: ${arg}`)
      flags.add(arg)
      continue
    }
    if (arg === '--help' || arg === '-h') {
      options.help = true
      continue
    }
    if (!['--content-type', '--source-file', '--batch-size', '--report-dir', '--rollback', '--state-file'].includes(arg)) {
      throw new Error(`unknown option: ${arg}`)
    }
    const value = argv[i + 1]
    if (!value || value.startsWith('--')) throw new Error(`missing value for ${arg}`)
    if (Object.hasOwn(options, arg)) throw new Error(`duplicate option: ${arg}`)
    options[arg] = value
    i += 1
  }
  if (options.help) return options
  if (flags.size !== 1) throw new Error('choose exactly one of --dry-run or --apply')
  options.mode = flags.has('--dry-run') ? 'dry-run' : 'apply'
  options.contentType = options['--content-type']
  options.sourceFile = options['--source-file']
  options.reportDir = options['--report-dir']
  options.stateFile = options['--state-file']
  options.rollbackBatch = options['--rollback']
  options.batchSize = options['--batch-size'] === undefined ? 20 : Number(options['--batch-size'])
  if (!Number.isInteger(options.batchSize) || options.batchSize < 1 || options.batchSize > MAX_BATCH_SIZE) {
    throw new Error(`--batch-size must be an integer from 1 to ${MAX_BATCH_SIZE}`)
  }
  if (!options.reportDir) throw new Error('--report-dir is required')
  if (options.rollbackBatch) {
    if (options.mode !== 'apply' || !options.stateFile || options.sourceFile || options.contentType) {
      throw new Error('--rollback requires --apply and --state-file only')
    }
    assertInsideReportDir(options.reportDir, options.stateFile)
    return options
  }
  if (!options.contentType || !options.sourceFile) throw new Error('--content-type and --source-file are required')
  if (options.stateFile) assertInsideReportDir(options.reportDir, options.stateFile)
  return options
}

async function atomicWriteJson(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true })
  const tempPath = `${filePath}.${process.pid}.${randomUUID()}.tmp`
  try {
    await writeFile(tempPath, `${JSON.stringify(value, null, 2)}\n`, { flag: 'wx' })
    await rename(tempPath, filePath)
  } catch (error) {
    await unlink(tempPath).catch(() => {})
    throw error
  }
}

function grouped(items, size) {
  const result = []
  for (let i = 0; i < items.length; i += size) result.push(items.slice(i, i + size))
  return result
}

async function main(argv = process.argv.slice(2)) {
  let options
  try {
    options = parseArgs(argv)
    if (options.help) {
      process.stdout.write('Local-only content staging importer\n' +
        '  --content-type article|product|video|tutorial|community\n' +
        '  --source-file <manifest.json> --dry-run|--apply --batch-size <1-500> --report-dir <dir>\n' +
        '  --apply --rollback <batch-id> --state-file <file> --report-dir <dir>\n' +
        'Writes only local JSON staging files; no database or network connection is supported.\n')
      return 0
    }
  } catch (error) {
    process.stderr.write(`${error.message}\n`)
    return 2
  }

  const reportDir = path.resolve(options.reportDir)
  const stateFile = path.resolve(options.stateFile || path.join(reportDir, 'staging-state.json'))
  try {
    if (options.rollbackBatch) {
      await assertRealPathInsideReportDir(reportDir, stateFile)
      const state = validateState(JSON.parse(await readFile(stateFile, 'utf8')))
      const originalOperations = state.operations.filter(item => item.batchId === options.rollbackBatch && item.action !== 'ROLLBACK')
      const rolledBackIds = new Set(state.operations.filter(item => item.action === 'ROLLBACK').map(item => item.rollbackOf))
      const alreadyRolledBack = originalOperations.length > 0 && originalOperations.every(item => rolledBackIds.has(item.operationId))
      const rolledBack = rollbackBatch(state, options.rollbackBatch)
      const reportFile = path.join(reportDir, `rollback-${options.rollbackBatch.replace(/[^a-zA-Z0-9_-]/g, '_')}.json`)
      const report = {
        tool: TOOL_NAME,
        target: 'local-staging-json',
        batchId: options.rollbackBatch,
        result: alreadyRolledBack ? 'ALREADY_ROLLED_BACK' : 'ROLLBACK_IN_PROGRESS',
        productionConnected: false
      }
      await atomicWriteJson(reportFile, report)
      if (!alreadyRolledBack) {
        await atomicWriteJson(stateFile, rolledBack)
        report.result = 'ROLLED_BACK'
        await atomicWriteJson(reportFile, report)
      }
      process.stdout.write(`ROLLBACK PASS ${options.rollbackBatch}\n`)
      return 0
    }

    await mkdir(reportDir, { recursive: true })
    await assertRealPathInsideReportDir(reportDir, stateFile)
    const manifestJson = await loadManifest(options.contentType, options.sourceFile)
    const manifest = validateManifest(options.contentType, manifestJson)
    let state = emptyState()
    try {
      state = validateState(JSON.parse(await readFile(stateFile, 'utf8')))
    } catch (error) {
      if (error.code !== 'ENOENT') throw error
    }

    const plan = buildPlan(options.contentType, manifest.records, state)
    const runId = new Date().toISOString().replace(/[:.]/g, '-') + '-' + randomUUID().slice(0, 8)
    const report = {
      tool: TOOL_NAME,
      runId,
      mode: options.mode,
      target: options.mode === 'dry-run' ? 'read-only-local-plan' : 'local-staging-json',
      productionConnected: false,
      contentType: manifest.contentType,
      sourceFile: path.resolve(options.sourceFile),
      sourceSha256: await hashSourceFile(options.sourceFile, options.contentType),
      manifestSha256: manifest.manifestHash,
      batchSize: options.batchSize,
      counts: Object.fromEntries([...new Set(plan.map(item => item.action))].map(action => [action, plan.filter(item => item.action === action).length])),
      plan
    }

    const reportFile = path.join(reportDir, `content-import-${options.mode}-${runId}.json`)
    if (options.mode === 'apply') {
      let nextState = state
      const batchIds = []
      for (const [batchIndex, batchRecords] of grouped(manifest.records, options.batchSize).entries()) {
        const batchPlan = buildPlan(options.contentType, batchRecords, nextState)
        if (!batchPlan.some(item => ['INSERT_STAGING', 'UPDATE_STAGING'].includes(item.action))) continue
        const batchId = `${runId}-${String(batchIndex + 1).padStart(3, '0')}`
        nextState = applyBatch(nextState, batchPlan, { batchId })
        batchIds.push(batchId)
      }
      report.appliedBatches = batchIds.length
      report.batchIds = batchIds
      report.stateFile = stateFile
      report.note = 'Applied to local staging JSON only; no database write, publication, or network connection occurred.'
      report.status = batchIds.length > 0 ? 'APPLYING' : 'NO_CHANGES'
      await atomicWriteJson(reportFile, report)
      if (batchIds.length > 0) {
        await atomicWriteJson(stateFile, nextState)
        report.status = 'APPLIED'
        await atomicWriteJson(reportFile, report)
      }
    } else {
      await atomicWriteJson(reportFile, report)
    }
    const counts = Object.entries(report.counts).map(([name, count]) => `${name}=${count}`).join(' ')
    process.stdout.write(`${options.mode.toUpperCase()} ${manifest.contentType} ${counts}\nREPORT ${reportFile}\n`)
    return 0
  } catch (error) {
    process.stderr.write(`IMPORT ERROR: ${error.message}\n`)
    return 1
  }
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (isMain) process.exitCode = await main()
