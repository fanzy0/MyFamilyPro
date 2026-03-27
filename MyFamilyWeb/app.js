// app.js
const userApi = require('./utils/userApi.js');
const familyApi = require('./utils/familyApi.js');

App({
  /**
   * 小程序启动时执行
   * 1. 初始化云开发环境
   * 2. 发起自动登录校验（调用 /api/user/me）：
   *    - 成功：用户已在系统注册，同步拉取家庭列表，通知登录页直接跳转
   *    - 失败：用户未注册或网络异常，登录页展示"微信一键登录"按钮等待用户操作
   */
  onLaunch() {
    this._initCloud();
    this._checkLoginStatus();
  },

  /**
   * 初始化微信云开发环境
   */
  _initCloud() {
    if (!wx.cloud) {
      console.error('[App] 请使用 2.2.3 或以上的基础库以使用云能力');
    } else {
      wx.cloud.init({
        env: 'prod-8gq9rikccd2b0066',
        traceUser: true
      });
      console.log('[App] 云开发环境初始化成功');
    }
  },

  /**
   * 启动时自动登录校验
   * 调用 GET /api/user/me，openid 由云托管平台自动携带
   *   - 成功：真实用户已注册，拉取家庭列表后通知跳转主页
   *   - 失败：用户未注册或已注销，自动静默临时登录，让用户先体验后再授权（符合平台规范）
   */
  _checkLoginStatus() {
    if (this._checkingAuth) {
      return;
    }
    this._checkingAuth = true;
    console.log('[App] 开始自动登录校验');

    userApi.getMe()
      .then(userInfo => {
        console.log('[App] 自动登录校验成功, userId=' + userInfo.id);
        this.globalData.currentUser = userInfo;
        this.globalData.isLoggedIn = true;
        this.globalData.isTempLogin = false;

        return familyApi.getMyFamilies()
          .then(list => {
            this.globalData.familyList = list || [];
            if (list && list.length > 0) {
              this.globalData.currentFamilyId = list[0].familyId;
            }
            console.log('[App] 家庭列表加载完成, count=' + (list || []).length);
          })
          .catch(() => {
            console.warn('[App] 家庭列表拉取失败，使用空列表');
            this.globalData.familyList = [];
          });
      })
      .then(() => {
        this._checkingAuth = false;
        this.globalData.authChecked = true;
        if (typeof this._onAuthChecked === 'function') {
          this._onAuthChecked(true);
          this._onAuthChecked = null;
        }
      })
      .catch(() => {
        this._checkingAuth = false;
        // 用户未注册或已注销：平台规范要求先让用户体验，不得强迫登录
        // 自动静默完成临时登录，用户进入体验首页（只读模式）
        console.log('[App] 真实用户校验未通过，自动临时登录进入体验模式');
        this._doTempLoginSilently();
      });
  },

  /**
   * 静默临时登录（仅供 app 启动时使用）
   * 使用固定演示账号 QINGJUXUNUAN001，让未注册用户直接体验功能
   * 体验模式下写操作会弹出登录弹窗引导用户授权
   */
  _doTempLoginSilently() {
    userApi.tempLogin()
      .then(res => {
        console.log('[App] 临时登录成功, userId=' + res.user.id);
        this.globalData.currentUser = res.user;
        this.globalData.familyList = res.familyList || [];
        if (res.familyList && res.familyList.length > 0) {
          this.globalData.currentFamilyId = res.familyList[0].familyId;
        }
        this.globalData.isLoggedIn = true;
        this.globalData.isTempLogin = true;
        this.globalData.authChecked = true;

        if (typeof this._onAuthChecked === 'function') {
          this._onAuthChecked(true);
          this._onAuthChecked = null;
        }
      })
      .catch(() => {
        // 临时登录也失败（网络问题等）：降级展示登录按钮
        console.warn('[App] 临时登录失败，降级展示登录界面');
        this.globalData.isLoggedIn = false;
        this.globalData.isTempLogin = false;
        this.globalData.authChecked = true;

        if (typeof this._onAuthChecked === 'function') {
          this._onAuthChecked(false);
          this._onAuthChecked = null;
        }
      });
  },

  /**
   * 清理用户数据（退出登录、注销、401 等场景调用）
   * 清理后 isTempLogin 也重置为 false，app 下次启动会重新走临时登录流程
   */
  clearUserData() {
    this.globalData.currentUser = null;
    this.globalData.familyList = [];
    this.globalData.currentFamilyId = null;
    this.globalData.isLoggedIn = false;
    this.globalData.isTempLogin = false;
    this.globalData.authChecked = false;
    console.log('[App] 用户数据已清理');
  },

  /**
   * 全局状态
   *
   * currentUser      当前登录用户信息（id、nickname、avatarUrl、status）
   * familyList       用户已加入的家庭列表
   * currentFamilyId  当前激活的家庭 ID
   * isLoggedIn       是否已登录（真实用户或临时用户均为 true）
   * isTempLogin      是否为临时体验用户（true 时写操作需弹窗引导登录）
   * authChecked      自动登录校验是否已完成
   */
  globalData: {
    currentUser: null,
    familyList: [],
    currentFamilyId: null,
    isLoggedIn: false,
    isTempLogin: false,
    authChecked: false
  }
});
