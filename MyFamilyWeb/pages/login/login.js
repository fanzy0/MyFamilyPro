// pages/login/login.js
const userApi = require('../../utils/userApi.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    loading: true,           // 是否正在进行自动登录校验（展示 loading 遮罩）
    loginLoading: false,     // 是否正在进行微信一键登录（按钮 loading 状态）
    tempLoginLoading: false, // 是否正在进行临时登录（按钮 loading 状态）
    agreed: false            // 是否已勾选用户协议
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
   * 切换协议勾选状态
   */
  onToggleAgreement() {
    this.setData({ agreed: !this.data.agreed });
  },

  /**
   * 点击协议链接，跳转协议阅读页
   * @param {Object} e 事件对象，e.currentTarget.dataset.type 为文件类型
   */
  onViewProtocol(e) {
    const type = e.currentTarget.dataset.type;
    wx.navigateTo({ url: `/pages/protocol/protocol?type=${type}` });
  },

  /**
   * 点击"微信一键登录"按钮
   * 调用 POST /api/auth/login，openid 由云托管平台自动注入，无需用户授权
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
   * 点击"临时登录"按钮
   * 弹出提示告知用户当前为临时账号，确认后使用固定 ID 完成登录
   */
  onTempLoginTap() {
    if (this.data.tempLoginLoading) return;

    wx.showModal({
      title: '临时账号提示',
      content: '您当前使用的是临时账号，请勿上传您个人图片等信息',
      confirmText: '我知道了',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this._doTempLogin();
        }
      }
    });
  },

  /**
   * 执行临时登录请求
   * 向后端传递固定 ID QINGJUXUNUAN001，后端以此查询临时账号信息
   */
  _doTempLogin() {
    this.setData({ tempLoginLoading: true });
    console.log('[Login] 开始临时登录');

    userApi.tempLogin()
      .then(res => {
        console.log('[Login] 临时登录成功, userId=' + res.user.id);

        const app = getApp();
        app.globalData.currentUser = res.user;
        app.globalData.familyList = res.familyList || [];
        app.globalData.isLoggedIn = true;
        app.globalData.authChecked = true;
        app.globalData.isTempLogin = true;  // 标记临时登录，后续所有请求自动携带 TEMP-OPENID

        this._navigateToMain();
      })
      .catch(err => {
        console.error('[Login] 临时登录失败:', err);
        this.setData({ tempLoginLoading: false });
        wx.showToast({
          title: '临时登录失败，请稍后重试',
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
