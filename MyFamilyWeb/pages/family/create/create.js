// pages/family/create/create.js
const familyApi = require('../../../utils/familyApi.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    familyName: '',           // 家庭名称输入值
    placeholderText: '例如：张三家庭', // 输入框占位文本
    meetDate: '',             // 相识相遇日期（可选，格式 YYYY-MM-DD）
    today: '',                // 日期选择器最大可选日期（今天）
    submitting: false         // 防止重复提交
  },

  /**
   * 生命周期函数--监听页面加载
   * 若当前用户有昵称，则将占位符改为"{nickname}家庭"
   * 同时计算今天日期作为日期选择器上限
   */
  onLoad() {
    const app = getApp();
    const nickname = app.globalData.currentUser && app.globalData.currentUser.nickname;
    if (nickname) {
      this.setData({ placeholderText: nickname + '家庭' });
    }

    const now = new Date();
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    this.setData({ today });
    console.log('[Create] 创建家庭页加载');
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
   * 监听家庭名称输入变化
   */
  onFamilyNameInput(e) {
    this.setData({ familyName: e.detail.value });
  },

  /**
   * 日期选择器变化：选择相识相遇日期
   */
  onMeetDateChange(e) {
    this.setData({ meetDate: e.detail.value });
    console.log('[Create] 选择相识日期:', e.detail.value);
  },

  /**
   * 清除相识日期
   */
  onClearMeetDate() {
    this.setData({ meetDate: '' });
  },

  /**
   * 点击「创建家庭」按钮
   * 1. 本地校验名称非空、长度（2~20字符）
   * 2. 调用 POST /api/family/create（携带 meetDate，可选）
   * 3. 成功后刷新 familyList，设置 currentFamilyId，跳转家庭主页
   */
  onCreateTap() {
    if (this.data.submitting) return;

    const familyName = this.data.familyName.trim();

    if (!familyName) {
      wx.showToast({ title: '请输入家庭名称', icon: 'none' });
      return;
    }
    if (familyName.length < 2) {
      wx.showToast({ title: '家庭名称至少2个字符', icon: 'none' });
      return;
    }
    if (familyName.length > 20) {
      wx.showToast({ title: '家庭名称不超过20个字符', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({ title: '创建中...', mask: true });
    console.log('[Create] 开始创建家庭, familyName=' + familyName + ', meetDate=' + this.data.meetDate);

    familyApi.createFamily(familyName, this.data.meetDate || null)
      .then(family => {
        console.log('[Create] 创建成功, familyId=' + family.familyId);
        const app = getApp();
        app.globalData.currentFamilyId = family.familyId;

        return familyApi.getMyFamilies().then(list => {
          app.globalData.familyList = list || [];
          console.log('[Create] 刷新 familyList 完成, count=' + (list || []).length);
        });
      })
      .then(() => {
        wx.hideLoading();
        this.setData({ submitting: false });
        wx.redirectTo({ url: '/pages/family/home/home' });
      })
      .catch(err => {
        wx.hideLoading();
        this.setData({ submitting: false });
        console.error('[Create] 创建家庭失败:', err);
        const msg = (err && err.data) || (err && err.message) || '创建失败，请稍后重试';
        wx.showToast({ title: msg, icon: 'none', duration: 2500 });
      });
  }
});
