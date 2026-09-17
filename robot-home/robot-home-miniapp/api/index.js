const { get, post, postForm, put, del } = require('../utils/request')

const homeApi = {
  index: function (position) {
    return get('/home/index', { position: position || 'miniapp' })
  }
}

const robotApi = {
  page: function (params) { return get('/robots', params) },
  filters: function () { return get('/robots/filters') },
  hot: function (limit) { return get('/robots/hot', { limit: limit || 10 }) },
  newest: function (limit) { return get('/robots/new', { limit: limit || 10 }) },
  compare: function (ids) { return get('/robots/compare', { ids: ids }) },
  detail: function (id) { return get('/robots/' + id) },
  params: function (id) { return get('/robots/' + id + '/params') },
  images: function (id) { return get('/robots/' + id + '/images') },
  videos: function (id) { return get('/robots/' + id + '/videos') },
  articles: function (id, limit) { return get('/robots/' + id + '/articles', { limit: limit || 10 }) },
  view: function (id) { return post('/robots/' + id + '/view') }
}

const brandApi = {
  page: function (params) { return get('/brands', params) },
  letters: function () { return get('/brands/letters') },
  hot: function (limit) { return get('/brands/hot', { limit: limit || 12 }) },
  detail: function (id) { return get('/brands/' + id) },
  robots: function (id, params) { return get('/brands/' + id + '/robots', params) }
}

const companyApi = {
  page: function (params) { return get('/companies', params) },
  regions: function () { return get('/companies/regions') },
  hot: function (limit) { return get('/companies/hot', { limit: limit || 10 }) },
  detail: function (id) { return get('/companies/' + id) },
  robots: function (id, params) { return get('/companies/' + id + '/robots', params) }
}

const articleApi = {
  page: function (params) { return get('/articles', params) },
  categories: function () { return get('/articles/categories') },
  hot: function (limit) { return get('/articles/hot', { limit: limit || 10 }) },
  detail: function (id) { return get('/articles/' + id) }
}

const videoApi = {
  page: function (params) { return get('/videos', params) },
  categories: function () { return get('/videos/categories') },
  hot: function (limit) { return get('/videos/hot', { limit: limit || 10 }) },
  detail: function (id) { return get('/videos/' + id) }
}

const tutorialApi = {
  page: function (params) { return get('/tutorials', params) },
  categories: function () { return get('/tutorials/categories') },
  hot: function (limit) { return get('/tutorials/hot', { limit: limit || 10 }) },
  detail: function (id) { return get('/tutorials/' + id) }
}

const communityApi = {
  circles: function () { return get('/community/circles') },
  posts: function (params) { return get('/community/posts', params) },
  topics: function (limit) { return get('/community/topics', { limit: limit || 10 }) },
  detail: function (id) { return get('/community/posts/' + id) },
  create: function (data) { return post('/community/posts', data) },
  remove: function (id) { return del('/community/posts/' + id) },
  myPosts: function (params) { return get('/community/my-posts', params) }
}

const commentApi = {
  list: function (params) { return get('/comments', params) },
  add: function (data) { return post('/comments', data) },
  like: function (id) { return post('/comments/' + id + '/like') }
}

const favoriteApi = {
  list: function (params) { return get('/favorites', params) },
  toggle: function (bizType, bizId) { return postForm('/favorites', { bizType: bizType, bizId: bizId }) },
  check: function (bizType, bizId) { return get('/favorites/check', { bizType: bizType, bizId: bizId }) }
}

const likeApi = {
  toggle: function (bizType, bizId) { return postForm('/likes', { bizType: bizType, bizId: bizId }) },
  check: function (bizType, bizId) { return get('/likes/check', { bizType: bizType, bizId: bizId }) }
}

const historyApi = {
  list: function (params) { return get('/history', params) },
  clear: function (bizType) { return del('/history', { bizType: bizType }) }
}

const searchApi = {
  search: function (keyword, limit) { return get('/search', { keyword: keyword, limit: limit || 5 }) },
  byType: function (type, params) { return get('/search/' + type, params) },
  suggest: function (keyword, limit) { return get('/search/suggest', { keyword: keyword, limit: limit || 10 }) },
  hot: function (limit) { return get('/search/hot', { limit: limit || 10 }) },
  history: function (limit) { return get('/search/history', { limit: limit || 10 }) },
  clearHistory: function () { return del('/search/history') }
}

const rankingApi = {
  rank: function (type, limit, timeRange) {
    var params = { type: type || 'hot', limit: limit || 20 }
    if (timeRange) params.timeRange = timeRange
    return get('/rankings', params)
  },
  types: function () { return get('/rankings/types') }
}

const inquiryApi = {
  submit: function (data) { return post('/inquiries', data) },
  my: function (params) { return get('/inquiries/my', params) },
  detail: function (id) { return get('/inquiries/my/' + id) }
}

const feedbackApi = {
  submit: function (data) { return post('/feedback', data) }
}

const behaviorApi = {
  track: function (action, bizType, bizId, extra) {
    return post('/behaviors', {
      action: action, bizType: bizType, bizId: bizId, extra: extra || {}
    }).catch(function () {})
  }
}

// Phase7: Review API
const reviewApi = {
  submit: function (data) { return post('/reviews', data) },
  update: function (id, data) { return put('/reviews/' + id, data) },
  remove: function (id) { return del('/reviews/' + id) },
  list: function (robotId, pageNum, pageSize) { return get('/reviews/robot/' + robotId, { pageNum: pageNum || 1, pageSize: pageSize || 20 }) },
  summary: function (robotId) { return get('/reviews/robot/' + robotId + '/summary') },
  helpful: function (id) { return post('/reviews/' + id + '/helpful') },
  unhelpful: function (id) { return del('/reviews/' + id + '/helpful') }
}

// Phase7: Correction API
const correctionApi = {
  submit: function (data) { return post('/corrections', data) },
  my: function (params) { return get('/corrections/my', params) }
}

// Phase7: Similar API
const similarApi = {
  list: function (robotId, limit) { return get('/robots/' + robotId + '/similar', { limit: limit || 6 }) }
}

// Phase8: Q&A API
const qaApi = {
  questions: function (params) { return get('/qa/questions', params) },
  questionDetail: function (id) { return get('/qa/questions/' + id) },
  ask: function (data) { return post('/qa/questions', data) },
  answers: function (questionId, params) { return get('/qa/questions/' + questionId + '/answers', params) },
  answer: function (questionId, data) { return post('/qa/questions/' + questionId + '/answers', data) },
  follow: function (id) { return post('/qa/questions/' + id + '/follow') },
  unfollow: function (id) { return del('/qa/questions/' + id + '/follow') },
  helpful: function (id) { return post('/qa/answers/' + id + '/helpful') }
}

// Phase8: Selection API
const selectionApi = {
  search: function (data) { return post('/robot-selection/search', data) },
  filters: function (category) { return get('/robot-selection/filters', { category: category || '' }) }
}

// Phase8: Procurement API
const procurementApi = {
  hallList: function (params) { return get('/procurement/hall', params) },
  hallDetail: function (id) { return get('/procurement/hall/' + id) }
}

const userApi = {
  me: function () { return get('/users/me') },
  update: function (data) { return put('/users/me', data) },
  stats: function () { return get('/users/me/stats') },
  myPosts: function (params) { return get('/users/me/posts', params) },
  myFavorites: function (params) { return get('/users/me/favorites', params) },
  myHistory: function (params) { return get('/users/me/history', params) }
}

const authApi = {
  login: function (username, password) {
    return postForm('/auth/login', { username: username, password: password })
  },
  smsLogin: function (phone, code) {
    return postForm('/auth/sms-login', { phone: phone, code: code })
  },
  sendSmsCode: function (phone, type) {
    return postForm('/auth/send-sms-code', { phone: phone, type: type || 'login' })
  },
  wxLogin: function (openid, nickname, avatar) {
    return postForm('/auth/wx-login', {
      openid: openid,
      nickname: nickname || '',
      avatar: avatar || ''
    })
  },
  logout: function () { return post('/auth/logout') },
  me: function () { return get('/auth/me') }
}

module.exports = {
  homeApi,
  robotApi,
  brandApi,
  companyApi,
  articleApi,
  videoApi,
  tutorialApi,
  communityApi,
  commentApi,
  favoriteApi,
  likeApi,
  historyApi,
  searchApi,
  rankingApi,
  inquiryApi,
  feedbackApi,
  behaviorApi,
  userApi,
  authApi,
  reviewApi,
  correctionApi,
  similarApi,
  qaApi,
  selectionApi,
  procurementApi
}
