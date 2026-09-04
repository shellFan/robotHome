const { baseUrl, tokenKey } = require('./config')

function getToken() {
  try {
    return wx.getStorageSync(tokenKey) || ''
  } catch (e) {
    return ''
  }
}

function setToken(token) {
  if (token) {
    wx.setStorageSync(tokenKey, token)
  } else {
    wx.removeStorageSync(tokenKey)
  }
}

function buildQuery(params) {
  if (!params) return ''
  const parts = []
  Object.keys(params).forEach(function (key) {
    const val = params[key]
    if (val === undefined || val === null || val === '') return
    parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(val))
  })
  return parts.length ? '?' + parts.join('&') : ''
}

/**
 * 统一请求：自动带 Bearer token，成功时解包 data
 * @param {object} options
 * @param {string} options.url 相对 /api 的路径，如 /robots
 * @param {string} [options.method]
 * @param {object} [options.data]
 * @param {boolean} [options.form] 使用 form-urlencoded（适配后端 @RequestParam）
 * @param {boolean} [options.silent] 不弹 toast
 */
function request(options) {
  const method = (options.method || 'GET').toUpperCase()
  const isForm = !!options.form
  let url = baseUrl + options.url
  let data = options.data || {}

  if (method === 'GET' || method === 'DELETE') {
    url += buildQuery(data)
    data = {}
  }

  const header = {
    'Content-Type': isForm ? 'application/x-www-form-urlencoded' : 'application/json'
  }
  const token = getToken()
  if (token) header.Authorization = 'Bearer ' + token

  return new Promise(function (resolve, reject) {
    wx.request({
      url: url,
      method: method,
      data: isForm && method !== 'GET' ? data : (method === 'GET' || method === 'DELETE' ? undefined : data),
      header: header,
      timeout: 20000,
      success: function (res) {
        const body = res.data
        if (res.statusCode === 401 || (body && body.code === 401)) {
          setToken('')
          if (!options.silent) {
            // 登录失效由页面自行引导，不强制打断
          }
          reject(new Error((body && body.message) || '未登录'))
          return
        }
        if (body && body.code === 200) {
          resolve(body.data)
          return
        }
        const message = (body && body.message) || '请求失败'
        if (!options.silent) {
          wx.showToast({ title: message, icon: 'none' })
        }
        reject(new Error(message))
      },
      fail: function (err) {
        if (!options.silent) {
          wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        }
        reject(err || new Error('网络异常'))
      }
    })
  })
}

function get(url, data, opts) {
  return request(Object.assign({ url: url, method: 'GET', data: data }, opts || {}))
}

function post(url, data, opts) {
  return request(Object.assign({ url: url, method: 'POST', data: data }, opts || {}))
}

/** 表单 POST，对应后端 @RequestParam */
function postForm(url, data, opts) {
  return request(Object.assign({ url: url, method: 'POST', data: data, form: true }, opts || {}))
}

function put(url, data, opts) {
  return request(Object.assign({ url: url, method: 'PUT', data: data }, opts || {}))
}

function del(url, data, opts) {
  return request(Object.assign({ url: url, method: 'DELETE', data: data }, opts || {}))
}

function ensureLogin() {
  if (getToken()) return true
  wx.navigateTo({ url: '/pages/login/login' })
  return false
}

module.exports = {
  request,
  get,
  post,
  postForm,
  put,
  del,
  getToken,
  setToken,
  ensureLogin
}
