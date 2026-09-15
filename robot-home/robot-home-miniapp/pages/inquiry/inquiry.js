const { inquiryApi } = require('../../api/index')
const { ensureLogin, getToken } = require('../../utils/request')
Page({
  data: {
    robotId: '', robotName: '',
    contactName: '', phone: '', company: '', content: '',
    quantity: 1, budget: '', remark: '',
    // Phase7 V2
    inquiryType: 'PRICE',
    procurementScene: '',
    purchaseTime: '',
    typeOptions: [
      { value: 'PRICE', label: '获取底价' },
      { value: 'PURCHASE', label: '我要采购' },
      { value: 'LEASE', label: '租赁咨询' },
      { value: 'COOPERATE', label: '合作洽谈' }
    ],
    sceneOptions: ['工业制造', '物流仓储', '医疗健康', '教育培训', '服务行业', '其他'],
    sceneValues: ['INDUSTRIAL', 'LOGISTICS', 'MEDICAL', 'EDUCATION', 'SERVICE', 'OTHER'],
    submitting: false
  },
  onLoad(q) {
    this.setData({
      robotId: q.robotId || '',
      robotName: q.robotName ? decodeURIComponent(q.robotName) : '',
      inquiryType: q.type || 'PRICE'
    })
  },
  onName(e) { this.setData({ contactName: e.detail.value }) },
  onPhone(e) { this.setData({ phone: e.detail.value }) },
  onCompany(e) { this.setData({ company: e.detail.value }) },
  onContent(e) { this.setData({ content: e.detail.value }) },
  onQuantity(e) { this.setData({ quantity: e.detail.value }) },
  onBudget(e) { this.setData({ budget: e.detail.value }) },
  onRemark(e) { this.setData({ remark: e.detail.value }) },
  onPurchaseTime(e) { this.setData({ purchaseTime: e.detail.value }) },
  onTypeChange(e) {
    this.setData({ inquiryType: this.data.typeOptions[e.detail.value].value })
  },
  onSceneChange(e) {
    this.setData({ procurementScene: this.data.sceneValues[e.detail.value] || '' })
  },
  async submit() {
    if (!getToken()) {
      ensureLogin()
      return
    }
    const contactName = (this.data.contactName || '').trim()
    const phone = (this.data.phone || '').trim()
    if (!contactName) return wx.showToast({ title: '请填写联系人', icon: 'none' })
    if (!phone) return wx.showToast({ title: '请填写手机号', icon: 'none' })
    if (this.data.submitting) return
    this.setData({ submitting: true })
    try {
      await inquiryApi.submit({
        robotId: Number(this.data.robotId) || undefined,
        name: contactName,
        phone: phone,
        companyName: (this.data.company || '').trim() || undefined,
        quantity: Number(this.data.quantity) || 1,
        budget: (this.data.budget || '').trim() || undefined,
        remark: (this.data.remark || '').trim() || undefined,
        inquiryType: this.data.inquiryType,
        procurementScene: this.data.procurementScene || undefined,
        purchaseTime: (this.data.purchaseTime || '').trim() || undefined
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 600)
    } catch (e) {
      this.setData({ submitting: false })
    }
  }
})