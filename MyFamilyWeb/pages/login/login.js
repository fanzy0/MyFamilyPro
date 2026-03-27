// pages/login/login.js
const userApi = require('../../utils/userApi.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    loading: true,         // 是否正在进行自动登录校验（展示 loading 遮罩）
    loginLoading: false,   // 是否正在进行微信一键登录（按钮 loading 状态）
    agreed: false          // 是否已勾选用户协议
  },

  /**
   * 生命周期函数--监听页面加载
   *
   * 流程说明：
   *   - app.js 启动时会自动完成认证校验（真实用户）或静默临时登录（新用户/注销用户）
   *   - 两种情况下 isLoggedIn 均为 true，登录页直接跳转主页，用户无感知
   *   - 只有极端网络故障导致临时登录也失败时，才展示登录按钮作为兜底
   */
  onLoad() {
    const app = getApp();

    if (app.globalData.authChecked) {
      this._handleAuthResult(app.globalData.isLoggedIn);
    } else {
      app._onAuthChecked = (isLoggedIn) => {
        this._handleAuthResult(isLoggedIn);
      };
      // 兼容 clearUserData 后直接进入登录页的场景：
      // 若此时没有自动校验在运行，主动触发一次，避免页面一直 loading
      if (typeof app._checkLoginStatus === 'function') {
        app._checkLoginStatus();
      } else {
        this.setData({ loading: false });
      }
    }
  },

  onReady() {},
  onShow() {},
  onHide() {},
  onUnload() {},

  /**
   * 处理登录校验结果
   * - 已登录（真实用户或临时用户）：直接跳转主页，用户无感知
   * - 未登录（网络异常降级）：展示微信一键登录按钮作为兜底
   *
   * @param {Boolean} isLoggedIn
   */
  _handleAuthResult(isLoggedIn) {
    if (isLoggedIn) {
      console.log('[Login] 登录校验通过，跳转主页');
      this._navigateToMain();
    } else {
      console.log('[Login] 登录校验失败（网络降级），展示登录按钮');
      this.setData({ loading: false });
    }
  },

  /**
   * 切换协议勾选状态
   */
  onToggleAgreement() {
    this.setData({ agreed: !this.data.agreed });
  },

  /**
   * 查看用户协议或隐私声明
   */
  onViewProtocol(e) {
    const type = e.currentTarget.dataset.type;
    wx.navigateTo({ url: `/pages/protocol/protocol?type=${type}` });
  },

  /**
   * 点击"微信一键登录"（网络降级兜底入口）
   */
  onLoginTap() {
    if (this.data.loginLoading) return;

    if (!this.data.agreed) {
      wx.showToast({
        title: '请先阅读并同意用户协议与隐私声明',
        icon: 'none',
        duration: 2000
      });
      return;
    }

    this.setData({ loginLoading: true });
    console.log('[Login] 开始微信登录');

    userApi.login()
      .then(res => {
        console.log('[Login] 登录成功, userId=' + res.user.id);

        const app = getApp();
        app.globalData.currentUser = res.user;
        app.globalData.familyList = res.familyList || [];
        app.globalData.isLoggedIn = true;
        app.globalData.isTempLogin = false;
        app.globalData.authChecked = true;

        if (res.familyList && res.familyList.length > 0) {
          app.globalData.currentFamilyId = res.familyList[0].familyId;
        }

        this._navigateToMain();
      })
      .catch(err => {
        console.error('[Login] 登录失败:', err);
        this.setData({ loginLoading: false });
        const backendCode = err && err.data && err.data.code;
        const backendMsg = err && err.data && err.data.message;
        if (backendCode === 'USER_NOT_FOUND') {
          wx.showToast({
            title: '未找到账号，请稍后重试',
            icon: 'none',
            duration: 2200
          });
          return;
        }
        if (backendCode === 'USER_DEACTIVATED') {
          wx.showModal({
            title: '账号已注销',
            content: backendMsg || '该账号已注销，是否重新登录激活？',
            confirmText: '重新登录',
            cancelText: '取消',
            success: (res) => {
              if (res.confirm) {
                this.onLoginTap();
              }
            }
          });
          return;
        }
        wx.showToast({
          title: backendMsg || '登录失败，请稍后重试',
          icon: 'none',
          duration: 2000
        });
      });
  },

  /**
   * 根据 familyList 决定跳转目标
   */
  _navigateToMain() {
    const app = getApp();
    const familyList = app.globalData.familyList || [];

    if (familyList.length > 0) {
      app.globalData.currentFamilyId = familyList[0].familyId;
      wx.redirectTo({ url: '/pages/family/home/home' });
    } else {
      wx.redirectTo({ url: '/pages/family/guide/guide' });
    }
  }
});
