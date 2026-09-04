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
  rank: function (type, limit) { return get('/rankings', { type: type || 'hot', limit: limit || 20 }) },
  types: function () { return get('/rankings/types') }
}

const inquiryApi = {
  submit: function (data) { return post('/inquiries', data) },
  my: function (params) { return get('/inquiries/my', params) },
  detail: function (id) { return get('/inquiries/my/' + id) }
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
  userApi,
  authApi
}
