// pages/family/join/join.js
const familyApi = require('../../../utils/familyApi.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    familyCode: '',         // 家庭编号输入值
    familyPreview: null,    // 查询到的家庭预览信息 {familyId, familyName, memberCount}
    querying: false,        // 查询中状态
    submitting: false       // 提交申请中状态
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad() {
    console.log('[Join] 加入家庭页加载');
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {},

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {},

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {},

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {},

  /**
   * 监听家庭编号输入
   * 输入变化时清空之前的预览信息
   */
  onFamilyCodeInput(e) {
    this.setData({
      familyCode: e.detail.value,
      familyPreview: null
    });
  },

  /**
   * 点击「查询家庭信息」
   * 调用 GET /api/family/by-code 查询家庭信息，展示预览卡片
   */
  onQueryFamily() {
    if (this.data.querying) return;

    const familyCode = this.data.familyCode.trim();
    if (!familyCode) {
      wx.showToast({ title: '请输入家庭编号', icon: 'none' });
      return;
    }

    this.setData({ querying: true, familyPreview: null });
    console.log('[Join] 查询家庭信息, familyCode=' + familyCode);

    familyApi.getFamilyByCode(familyCode)
      .then(family => {
        console.log('[Join] 查询成功, familyName=' + family.familyName);
        this.setData({
          familyPreview: family,
          querying: false
        });
      })
      .catch(err => {
        this.setData({ querying: false });
        console.error('[Join] 查询家庭失败:', err);
        if (err && err.code === 404) {
          wx.showToast({ title: '家庭不存在或编号错误', icon: 'none', duration: 2000 });
        } else {
          wx.showToast({ title: '查询失败，请稍后重试', icon: 'none', duration: 2000 });
        }
      });
  },

  /**
   * 点击「申请加入」
   * 先确认已有预览，再调用 POST /api/family/join 提交申请
   */
  onApplyJoin() {
    if (this.data.submitting) return;

    if (!this.data.familyPreview) {
      wx.showToast({ title: '请先查询家庭信息', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({ title: '提交申请中...', mask: true });
    console.log('[Join] 提交加入申请, familyCode=' + this.data.familyCode);

    familyApi.joinFamily(this.data.familyCode.trim())
      .then(() => {
        wx.hideLoading();
        this.setData({ submitting: false });
        console.log('[Join] 申请提交成功');
        wx.showToast({ title: '已提交申请，等待户主审批', icon: 'success', duration: 2000 });
        setTimeout(() => {
          wx.navigateBack();
        }, 2000);
      })
      .catch(err => {
        wx.hideLoading();
        this.setData({ submitting: false });
        console.error('[Join] 申请加入失败:', err);

        const code = err && err.code;
        if (code === 409) {
          const msg = (err.data) || '您已申请或已加入该家庭';
          wx.showModal({
            title: '提示',
            content: msg,
            showCancel: false,
            confirmText: '知道了'
          });
        } else {
          wx.showToast({ title: (err && err.message) || '申请失败，请稍后重试', icon: 'none', duration: 2500 });
        }
      });
  }
});
