import request from '@/utils/request'

/**
 * 文件上传：POST /api/files/upload，multipart 字段名 file，额外字段 module
 * 返回 { url, name, size }
 */
export function uploadFile(formData, config) {
  return request(
    Object.assign(
      {
        url: '/files/upload',
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
      },
      config || {}
    )
  )
}
