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

/** 企业认领申请分页 */
export function getCompanyClaimPage(params) {
  return request({ url: '/admin/company-claims', method: 'get', params })
}

/** 审核企业认领申请（后端使用 RequestParam） */
export function reviewCompanyClaim(claimId, approved, reviewComment) {
  return request({
    url: '/admin/company-claims/review',
    method: 'post',
    params: { claimId, approved, reviewComment }
  })
}

/** 企业资料变更申请分页 */
export function getCompanyProfileChangePage(params) {
  return request({ url: '/admin/company-profile-changes', method: 'get', params })
}

/** 审核企业资料变更申请（后端使用 RequestParam） */
export function reviewCompanyProfileChange(changeId, approved, reviewComment) {
  return request({
    url: '/admin/company-profile-changes/review',
    method: 'post',
    params: { changeId, approved, reviewComment }
  })
}

export const COMPANY_REVIEW_STATUS_MAP = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' }
}
