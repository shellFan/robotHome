/**
 * Create robot_home database and import SQL files.
 * Usage: node scripts/setup-db.js
 *
 * 所有凭据通过环境变量传入，不硬编码任何密码：
 *   MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD
 *   REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
const fs = require('fs')
const path = require('path')
const mysql = require('mysql2/promise')
const Redis = require('ioredis')

const MYSQL = {
  host: process.env.MYSQL_HOST || 'localhost',
  port: parseInt(process.env.MYSQL_PORT || '3306', 10),
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || ''
}

const REDIS = {
  host: process.env.REDIS_HOST || 'localhost',
  port: parseInt(process.env.REDIS_PORT || '6379', 10),
  password: process.env.REDIS_PASSWORD || ''
}

const SQL_DIR = path.join(__dirname, '..', 'sql')
const FILES = ['01_schema.sql', '02_init_data.sql', '03_demo_data.sql']

async function testRedis() {
  const redis = new Redis({
    ...REDIS,
    connectTimeout: 8000,
    maxRetriesPerRequest: 1,
    lazyConnect: true
  })
  try {
    await redis.connect()
    const pong = await redis.ping()
    await redis.set('robot_home:setup', String(Date.now()), 'EX', 120)
    console.log('[OK] Redis ping =', pong)
  } finally {
    redis.disconnect()
  }
}

async function importSql() {
  const root = await mysql.createConnection({
    ...MYSQL,
    multipleStatements: true,
    connectTimeout: 20000
  })
  try {
    await root.query(
      'CREATE DATABASE IF NOT EXISTS robot_home DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
    )
    console.log('[OK] DATABASE robot_home ready')
  } finally {
    await root.end()
  }

  const conn = await mysql.createConnection({
    ...MYSQL,
    database: 'robot_home',
    multipleStatements: true,
    connectTimeout: 60000
  })
  try {
    // Drop all tables for clean re-import
    await conn.query('SET FOREIGN_KEY_CHECKS=0')
    const [tables] = await conn.query(
      "SELECT table_name AS t FROM information_schema.tables WHERE table_schema='robot_home'"
    )
    for (const row of tables) {
      const name = row.t || row.T
      await conn.query('DROP TABLE IF EXISTS `' + name + '`')
    }
    console.log('[OK] dropped', tables.length, 'old tables')

    for (const file of FILES) {
      const full = path.join(SQL_DIR, file)
      let sql = fs.readFileSync(full, 'utf8')
      // Remove USE statements that might conflict
      sql = sql.replace(/USE\s+[`']?robot_home[`']?\s*;/gi, '')
      console.log('[..] importing', file, '(' + Math.round(sql.length / 1024) + ' KB)')
      await conn.query(sql)
      console.log('[OK]', file)
    }

    const [countRows] = await conn.query(
      "SELECT table_name AS t, table_rows AS c FROM information_schema.tables WHERE table_schema='robot_home' ORDER BY table_name"
    )
    console.log('[OK] tables:', countRows.length)
    const [robots] = await conn.query('SELECT COUNT(*) AS c FROM robot WHERE deleted=0')
    const [brands] = await conn.query('SELECT COUNT(*) AS c FROM brand WHERE deleted=0')
    const [articles] = await conn.query('SELECT COUNT(*) AS c FROM article WHERE deleted=0')
    console.log('[OK] robots=', robots[0].c, 'brands=', brands[0].c, 'articles=', articles[0].c)
  } finally {
    await conn.end()
  }
}

;(async () => {
  try {
    await testRedis()
    await importSql()
    console.log('[DONE] setup complete')
  } catch (e) {
    console.error('[FAIL]', e.message)
    process.exit(1)
  }
})()