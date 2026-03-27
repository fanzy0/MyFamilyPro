// components/login-modal/login-modal.js
const userApi = require('../../utils/userApi.js');
const familyApi = require('../../utils/familyApi.js');

Component({
  /**
   * 登录弹窗组件
   * 
   * 使用方式：
   *   1. 在页面 JSON 注册：{ "usingComponents": { "login-modal": "/components/login-modal/login-modal" } }
   *   2. 在 WXML 引入：<login-modal id="loginModal" bind:loginSuccess="onLoginSuccess"></login-modal>
   *   3. 需要登录时调用：this.selectComponent('#loginModal').show()
   * 
   * 事件：
   *   loginSuccess  登录成功，detail = { user, familyList }
   *   cancel        用户点击"暂不登录"或遮罩关闭
   */
  data: {
    visible: false,
    agreed: false,
    loginLoading: false
  },

  methods: {
    /**
     * 显示弹窗（外部调用）
     */
    show() {
      this.setData({ visible: true, agreed: false, loginLoading: false });
    },

    /**
     * 隐藏弹窗
     */
    hide() {
      this.setData({ visible: false });
    },

    /**
     * 点击遮罩：关闭弹窗（相当于取消）
     */
    onMaskTap() {
      this._cancel();
    },

    /**
     * 阻止面板内部点击冒泡到遮罩
     */
    onPanelTap() {},

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
     * 点击"取消"按钮
     */
    onCancel() {
      this._cancel();
    },

    /**
     * 内部取消逻辑
     */
    _cancel() {
      this.hide();
      this.triggerEvent('cancel');
    },

    /**
     * 点击"微信一键登录"
     * 调用 POST /api/auth/login，openid 由云托管平台自动注入
     * 登录成功后更新 globalData，触发 loginSuccess 事件
     */
    onLoginTap() {
      if (!this.data.agreed) {
        wx.showToast({
          title: '请先阅读并同意用户协议与隐私声明',
          icon: 'none',
          duration: 2000
        });
        return;
      }
      if (this.data.loginLoading) return;

      this.setData({ loginLoading: true });
      console.log('[LoginModal] 开始微信登录');

      userApi.login()
        .then(res => {
          console.log('[LoginModal] 登录成功, userId=' + res.user.id);

          const app = getApp();
          app.globalData.currentUser = res.user;
          app.globalData.familyList = res.familyList || [];
          app.globalData.isLoggedIn = true;
          app.globalData.authChecked = true;
          app.globalData.isTempLogin = false;

          if (res.familyList && res.familyList.length > 0) {
            app.globalData.currentFamilyId = res.familyList[0].familyId;
          }

          this.setData({ loginLoading: false });
          this.hide();
          this.triggerEvent('loginSuccess', {
            user: res.user,
            familyList: res.familyList || []
          });
        })
        .catch(err => {
          console.error('[LoginModal] 登录失败:', err);
          this.setData({ loginLoading: false });
          const backendCode = err && err.data && err.data.code;
          const backendMsg = err && err.data && err.data.message;
          if (backendCode === 'USER_NOT_FOUND') {
            wx.showToast({
              title: '登录失败，请重试',
              icon: 'none',
              duration: 2000
            });
            return;
          }
          if (backendCode === 'USER_DEACTIVATED') {
            wx.showModal({
              title: '账号已注销',
              content: backendMsg || '该账号已注销，是否再次登录？',
              confirmText: '再次登录',
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
    }
  }
});
