import request from '@/utils/request'

/** 统一封装：GET 带参数的请求 */
const get = (url, params) => request.get(url, { params })
const post = (url, data) => request.post(url, data)
const postForm = (url, params) => request.post(url, null, { params })
const del = (url) => request.delete(url)
const put = (url, data) => request.put(url, data)

export const authApi = {
  login: (data) => postForm('/auth/login', data),
  register: (data) => postForm('/auth/register', data),
  smsLogin: (data) => postForm('/auth/sms-login', data),
  sendSmsCode: (phone, type = 'login') => postForm('/auth/send-sms-code', { phone, type }),
  sendSms: (phone, scene) => postForm('/auth/send-sms-code', { phone, type: scene || 'login' }),
  captcha: () => get('/auth/captcha'),
  refresh: (refreshToken) => postForm('/auth/refresh', { refreshToken }),
  logout: () => post('/auth/logout'),
  me: () => get('/auth/me')
}

export const homeApi = {
  index: (position = 'pc') => get('/home/index', { position })
}

export const robotApi = {
  page: (params) => get('/robots', params),
  filters: () => get('/robots/filters'),
  hot: (limit = 10) => get('/robots/hot', { limit }),
  newest: (limit = 10) => get('/robots/new', { limit }),
  compare: (ids) => get('/robots/compare', { ids }),
  detail: (id) => get(`/robots/${id}`),
  params: (id) => get(`/robots/${id}/params`),
  images: (id) => get(`/robots/${id}/images`),
  videos: (id) => get(`/robots/${id}/videos`),
  articles: (id, limit = 10) => get(`/robots/${id}/articles`, { limit }),
  view: (id) => post(`/robots/${id}/view`)
}

export const brandApi = {
  page: (params) => get('/brands', params),
  letters: () => get('/brands/letters'),
  hot: (limit = 12) => get('/brands/hot', { limit }),
  detail: (id) => get(`/brands/${id}`),
  robots: (id, params) => get(`/brands/${id}/robots`, params)
}

export const companyApi = {
  page: (params) => get('/companies', params),
  regions: () => get('/companies/regions'),
  hot: (limit = 10) => get('/companies/hot', { limit }),
  detail: (id) => get(`/companies/${id}`),
  robots: (id, params) => get(`/companies/${id}/robots`, params)
}

export const articleApi = {
  page: (params) => get('/articles', params),
  categories: () => get('/articles/categories'),
  hot: (limit = 10) => get('/articles/hot', { limit }),
  detail: (id) => get(`/articles/${id}`)
}

export const videoApi = {
  page: (params) => get('/videos', params),
  categories: () => get('/videos/categories'),
  hot: (limit = 10) => get('/videos/hot', { limit }),
  detail: (id) => get(`/videos/${id}`)
}

export const tutorialApi = {
  page: (params) => get('/tutorials', params),
  categories: () => get('/tutorials/categories'),
  hot: (limit = 10) => get('/tutorials/hot', { limit }),
  detail: (id) => get(`/tutorials/${id}`)
}

export const communityApi = {
  circles: () => get('/community/circles'),
  posts: (params) => get('/community/posts', params),
  topics: (limit = 10) => get('/community/topics', { limit }),
  detail: (id) => get(`/community/posts/${id}`),
  create: (data) => post('/community/posts', data),
  remove: (id) => del(`/community/posts/${id}`),
  myPosts: (params) => get('/community/my-posts', params)
}

export const commentApi = {
  list: (params) => get('/comments', params),
  replies: (id, params) => get(`/comments/${id}/replies`, params),
  add: (data) => post('/comments', data),
  remove: (id) => del(`/comments/${id}`),
  like: (id) => post(`/comments/${id}/like`),
  my: (params) => get('/comments/my', params),
  count: (bizType, bizId) => get('/comments/count', { bizType, bizId })
}

export const favoriteApi = {
  list: (params) => get('/favorites', params),
  toggle: (bizType, bizId) => postForm('/favorites', { bizType, bizId }),
  remove: (bizType, bizId) => del(`/favorites/${bizType}/${bizId}`),
  check: (bizType, bizId) => get('/favorites/check', { bizType, bizId }),
  count: (bizType) => get('/favorites/count', { bizType })
}

export const likeApi = {
  toggle: (bizType, bizId) => postForm('/likes', { bizType, bizId }),
  check: (bizType, bizId) => get('/likes/check', { bizType, bizId }),
  count: () => get('/likes/count')
}

export const followApi = {
  list: (params) => get('/follows', params),
  toggle: (followType, followId) => postForm('/follows', { followType, followId }),
  remove: (followType, followId) => del(`/follows/${followType}/${followId}`),
  check: (followType, followId) => get('/follows/check', { followType, followId })
}

export const historyApi = {
  list: (params) => get('/history', params),
  remove: (bizType, bizId) => del(`/history/${bizType}/${bizId}`),
  clear: (bizType) => request.delete('/history', { params: { bizType } })
}

export const searchApi = {
  search: (keyword, limit = 5) => get('/search', { keyword, limit }),
  byType: (type, params) => get(`/search/${type}`, params),
  suggest: (keyword, limit = 10) => get('/search/suggest', { keyword, limit }),
  hot: (limit = 10) => get('/search/hot', { limit }),
  history: (limit = 10) => get('/search/history', { limit }),
  clearHistory: () => del('/search/history')
}

export const rankingApi = {
  rank: (type = 'hot', limit = 20, timeRange = 'all') => get('/rankings', { type, limit, timeRange }),
  types: () => get('/rankings/types')
}

export const inquiryApi = {
  submit: (data) => post('/inquiries', data),
  my: (params) => get('/inquiries/my', params),
  detail: (id) => get(`/inquiries/my/${id}`)
}

export const behaviorApi = {
  /** 单条行为事件上报 (VIEW/SEARCH/CLICK/COMPARE/SHARE 由前端提交; FAVORITE/INQUIRY 由服务端产生) */
  track: (eventType, bizType, bizId, extra) => post('/behavior/event', { eventType, bizType, bizId, extra }),
  /** 批量行为事件上报 */
  batch: (events) => post('/behavior/batch', events),
  /** 查询对象热度分 */
  hotScore: (bizType, bizId) => get('/behavior/hot/score', { bizType, bizId }),
  /** 查询热度排行榜 Top N */
  hotTop: (bizType, limit = 20) => get('/behavior/hot/top', { bizType, limit })
}

export const feedbackApi = {
  /** 提交用户反馈 (bug/feature/improvement/other) */
  submit: (data) => post('/feedback/submit', data)
}

export const messageApi = {
  list: (params) => get('/messages', params),
  unreadCount: () => get('/messages/unread-count'),
  read: (id) => post(`/messages/${id}/read`),
  readAll: () => post('/messages/read-all')
}

export const userApi = {
  me: () => get('/users/me'),
  update: (data) => put('/users/me', data),
  stats: () => get('/users/me/stats'),
  profile: (id) => get(`/users/${id}`),
  myPosts: (params) => get('/users/me/posts', params),
  myComments: (params) => get('/users/me/comments', params),
  myFavorites: (params) => get('/users/me/favorites', params),
  myHistory: (params) => get('/users/me/history', params),
  myFollows: (params) => get('/users/me/follows', params)
}

export const bannerApi = {
  list: (position = 'pc') => get('/banners', { position })
}

export const recommendApi = {
  items: (code) => get(`/recommends/${code}`)
}

export const fileApi = {
  upload (file, module = 'common') {
    const form = new FormData()
    form.append('file', file)
    form.append('module', module)
    return request.post('/files/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
  }
}

export const systemApi = {
  siteConfig: () => get('/admin/system/site-config')
}
