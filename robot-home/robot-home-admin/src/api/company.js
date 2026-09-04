import request from '@/utils/request'

export function getCompanyPage(params) {
  return request({ url: '/admin/companies', method: 'get', params })
}

/** 全部启用企业（下拉用，返回数组） */
export function getCompanyAll() {
  return request({ url: '/admin/companies/all', method: 'get' })
}

export function getCompanyDetail(id) {
  return request({ url: `/admin/companies/${id}`, method: 'get' })
}

export function saveCompany(data) {
  return request({ url: '/admin/companies', method: 'post', data })
}

export function deleteCompany(id) {
  return request({ url: `/admin/companies/${id}`, method: 'delete' })
}
