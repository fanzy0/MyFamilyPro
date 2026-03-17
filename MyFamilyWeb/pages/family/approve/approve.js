// pages/family/approve/approve.js
const familyApi = require('../../../utils/familyApi.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    familyId: null,       // 当前激活家庭ID
    pendingList: [],       // 待审批成员列表
    loading: true,         // 页面加载状态
    operatingId: null      // 当前正在操作的 familyMemberId（防止重复点击）
  },

  /**
   * 生命周期函数--监听页面加载
   * 读取 currentFamilyId，加载待审批列表
   */
  onLoad() {
    const app = getApp();
    const familyId = app.globalData.currentFamilyId;
    if (!familyId) {
      wx.showToast({ title: '请先选择家庭', icon: 'none', duration: 2000 });
      setTimeout(() => wx.navigateBack(), 1500);
      return;
    }
    this.setData({ familyId });
    console.log('[Approve] 审批页加载, familyId=' + familyId);
    this._loadPendingList();
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {},

  /**
   * 生命周期函数--监听页面显示
   * 每次显示时刷新列表（从其他页面返回时也能更新）
   */
  onShow() {
    if (this.data.familyId) {
      this._loadPendingList();
    }
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {},

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {},

  /**
   * 页面下拉刷新
   */
  onPullDownRefresh() {
    this._loadPendingList().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 加载待审批列表
   * 调用 GET /api/family/pending-members?familyId=xxx
   * @return {Promise}
   */
  _loadPendingList() {
    this.setData({ loading: true });
    console.log('[Approve] 加载待审批列表, familyId=' + this.data.familyId);

    return familyApi.getPendingMembers(this.data.familyId)
      .then(list => {
        console.log('[Approve] 待审批成员数量=' + (list || []).length);
        this.setData({
          pendingList: list || [],
          loading: false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        console.error('[Approve] 加载失败:', err);
        if (err && err.code === 403) {
          wx.showToast({ title: '仅户主可访问该页面', icon: 'none', duration: 2000 });
          setTimeout(() => wx.navigateBack(), 1500);
        }
      });
  },

  /**
   * 格式化申请时间显示
   * @param {String} timeStr ISO时间字符串
   * @return {String} 格式化后的时间
   */
  _formatTime(timeStr) {
    if (!timeStr) return '';
    try {
      const d = new Date(timeStr);
      const M = d.getMonth() + 1;
      const D = d.getDate();
      const h = d.getHours();
      const m = String(d.getMinutes()).padStart(2, '0');
      return `${M}月${D}日 ${h}:${m}`;
    } catch (e) {
      return timeStr;
    }
  },

  /**
   * 点击「同意」按钮
   * 调用 POST /api/family/approve，成功后从列表移除该项
   */
  onApproveTap(e) {
    const { familyMemberId, index } = e.currentTarget.dataset;
    if (this.data.operatingId === familyMemberId) return;

    wx.showModal({
      title: '确认同意',
      content: '同意该成员加入家庭？',
      confirmText: '同意',
      confirmColor: '#3d9ae8',
      success: (res) => {
        if (!res.confirm) return;
        this._doApprove(familyMemberId, index);
      }
    });
  },

  /**
   * 执行审批通过操作
   */
  _doApprove(familyMemberId, index) {
    this.setData({ operatingId: familyMemberId });
    console.log('[Approve] 审批通过, familyMemberId=' + familyMemberId);

    familyApi.approveJoin(this.data.familyId, familyMemberId)
      .then(() => {
        console.log('[Approve] 审批通过成功');
        const list = this.data.pendingList.filter((_, i) => i !== index);
        this.setData({ pendingList: list, operatingId: null });
        wx.showToast({ title: '已同意加入', icon: 'success', duration: 1500 });
      })
      .catch(err => {
        this.setData({ operatingId: null });
        console.error('[Approve] 审批通过失败:', err);
        if (err && err.code === 409) {
          wx.showToast({ title: '成员状态已变化，请刷新后重试', icon: 'none', duration: 2000 });
          this._loadPendingList();
        }
      });
  },

  /**
   * 点击「拒绝」按钮
   * 调用 POST /api/family/reject，成功后从列表移除该项
   */
  onRejectTap(e) {
    const { familyMemberId, index } = e.currentTarget.dataset;
    if (this.data.operatingId === familyMemberId) return;

    wx.showModal({
      title: '确认拒绝',
      content: '拒绝该成员的加入申请？',
      confirmText: '拒绝',
      confirmColor: '#e74c3c',
      success: (res) => {
        if (!res.confirm) return;
        this._doReject(familyMemberId, index);
      }
    });
  },

  /**
   * 执行拒绝操作
   */
  _doReject(familyMemberId, index) {
    this.setData({ operatingId: familyMemberId });
    console.log('[Approve] 拒绝加入, familyMemberId=' + familyMemberId);

    familyApi.rejectJoin(this.data.familyId, familyMemberId)
      .then(() => {
        console.log('[Approve] 拒绝成功');
        const list = this.data.pendingList.filter((_, i) => i !== index);
        this.setData({ pendingList: list, operatingId: null });
        wx.showToast({ title: '已拒绝加入', icon: 'none', duration: 1500 });
      })
      .catch(err => {
        this.setData({ operatingId: null });
        console.error('[Approve] 拒绝失败:', err);
        if (err && err.code === 409) {
          wx.showToast({ title: '成员状态已变化，请刷新后重试', icon: 'none', duration: 2000 });
          this._loadPendingList();
        }
      });
  }
});
