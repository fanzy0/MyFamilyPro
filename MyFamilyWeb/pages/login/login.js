// pages/login/login.js
const userApi = require('../../utils/userApi.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    loading: true,      // 是否正在进行自动登录校验（展示 loading 遮罩）
    loginLoading: false // 是否正在进行手动登录（按钮 loading 状态）
  },

  /**
   * 生命周期函数--监听页面加载
   * 检查 app.js 的自动登录校验结果：
   *   - 已完成且已登录：直接跳转主页
   *   - 已完成但未登录：隐藏 loading，展示登录按钮
   *   - 尚未完成：注册回调，等待结果后再处理
   */
  onLoad() {
    const app = getApp();

    if (app.globalData.authChecked) {
      this._handleAuthResult(app.globalData.isLoggedIn);
    } else {
      app._onAuthChecked = (isLoggedIn) => {
        this._handleAuthResult(isLoggedIn);
      };
    }
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
   * 处理自动登录校验结果
   * @param {Boolean} isLoggedIn 是否已登录
   */
  _handleAuthResult(isLoggedIn) {
    if (isLoggedIn) {
      console.log('[Login] 自动登录校验通过，跳转主页');
      this._navigateToMain();
    } else {
      console.log('[Login] 用户未登录，展示登录按钮');
      this.setData({ loading: false });
    }
  },

  /**
   * 点击"微信一键登录"按钮
   * 调用 POST /api/auth/login，openid 由云托管平台自动注入，无需用户授权
   */
  onLoginTap() {
    if (this.data.loginLoading) return;

    this.setData({ loginLoading: true });
    console.log('[Login] 开始登录');

    userApi.login()
      .then(res => {
        console.log('[Login] 登录成功, userId=' + res.user.id);

        const app = getApp();
        app.globalData.currentUser = res.user;
        app.globalData.familyList = res.familyList || [];
        app.globalData.isLoggedIn = true;
        app.globalData.authChecked = true;

        this._navigateToMain();
      })
      .catch(err => {
        console.error('[Login] 登录失败:', err);
        this.setData({ loginLoading: false });
        wx.showToast({
          title: '登录失败，请稍后重试',
          icon: 'none',
          duration: 2000
        });
      });
  },

  /**
   * 根据 familyList 决定跳转目标
   * familyList 为空 → 跳转家庭引导页（创建/加入家庭）
   * familyList 非空 → 设置 currentFamilyId，跳转家庭主页
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
