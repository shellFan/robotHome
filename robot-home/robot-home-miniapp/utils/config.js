/**
 * 小程序全局配置
 * 根据 wx.getAccountInfoSync().miniProgram.envVersion 自动切换环境：
 *   develop  → 开发环境（本地/测试服务器）
 *   trial    → 体验版（测试服务器）
 *   release  → 正式版（生产服务器）
 *
 * 生产环境 baseUrl 必须在微信后台配置合法域名
 */
const envConfig = {
  develop: {
    baseUrl: 'http://localhost:8081/api'
  },
  trial: {
    baseUrl: 'https://test-api.robot-home.com/api'
  },
  release: {
    baseUrl: 'https://api.robot-home.com/api'
  }
}

function getEnv() {
  try {
    const accountInfo = wx.getAccountInfoSync()
    return accountInfo.miniProgram.envVersion || 'develop'
  } catch (e) {
    // 兼容旧版基础库
    return 'develop'
  }
}

const currentEnv = getEnv()
const config = envConfig[currentEnv] || envConfig.develop

module.exports = {
  baseUrl: config.baseUrl,
  envVersion: currentEnv,
  tokenKey: 'rh_token',
  userKey: 'rh_user',
  compareKey: 'rh_compare_ids',
  compareMax: 4,
  placeholderImage: '/assets/placeholder.png'
}