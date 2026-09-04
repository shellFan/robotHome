/**
 * 小程序全局配置
 * 真机调试时把 baseUrl 改成电脑局域网 IP，例如 http://192.168.1.8:8081/api
 */
module.exports = {
  baseUrl: 'http://localhost:8081/api',
  tokenKey: 'rh_token',
  userKey: 'rh_user',
  compareKey: 'rh_compare_ids',
  compareMax: 4,
  placeholderImage: '/assets/placeholder.png'
}
