const { inquiryApi } = require('../../api/index')
const { ensureLogin, getToken } = require('../../utils/request')
Page({
  data: {
    robotId: '', robotName: '',
    contactName: '', phone: '', company: '', content: '',
    submitting: false
  },
  onLoad(q) {
    this.setData({
      robotId: q.robotId || '',
      robotName: q.robotName ? decodeURIComponent(q.robotName) : ''
    })
  },
  onName(e) { this.setData({ contactName: e.detail.value }) },
  onPhone(e) { this.setData({ phone: e.detail.value }) },
  onCompany(e) { this.setData({ company: e.detail.value }) },
  onContent(e) { this.setData({ content: e.detail.value }) },
  async submit() {
    if (!getToken()) {
      ensureLogin()
      return
    }
    const contactName = (this.data.contactName || '').trim()
    const phone = (this.data.phone || '').trim()
    const content = (this.data.content || '').trim()
    if (!contactName) return wx.showToast({ title: '请填写联系人', icon: 'none' })
    if (!phone) return wx.showToast({ title: '请填写手机号', icon: 'none' })
    if (!content) return wx.showToast({ title: '请填写需求', icon: 'none' })
    if (this.data.submitting) return
    this.setData({ submitting: true })
    try {
      await inquiryApi.submit({
        robotId: this.data.robotId || undefined,
        robotName: this.data.robotName || undefined,
        contactName: contactName,
        phone: phone,
        company: (this.data.company || '').trim() || undefined,
        content: content
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 600)
    } catch (e) {
      this.setData({ submitting: false })
    }
  }
})
