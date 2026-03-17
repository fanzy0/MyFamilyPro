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
   * 成功后再调用 GET /api/family/my-families 填充家庭列表
   * 失败则用户需要在登录页点击按钮完成首次登录
   */
  _checkLoginStatus() {
    console.log('[App] 开始自动登录校验');

    userApi.getMe()
      .then(userInfo => {
        console.log('[App] 自动登录校验成功, userId=' + userInfo.id);
        this.globalData.currentUser = userInfo;
        this.globalData.isLoggedIn = true;

        // 拉取家庭列表，填充后再通知登录页跳转
        return familyApi.getMyFamilies()
          .then(list => {
            this.globalData.familyList = list || [];
            if (list && list.length > 0) {
              this.globalData.currentFamilyId = list[0].familyId;
            }
            console.log('[App] 家庭列表加载完成, count=' + (list || []).length);
          })
          .catch(() => {
            // 家庭列表拉取失败不影响登录主流程，保持空数组
            console.warn('[App] 家庭列表拉取失败，使用空列表');
            this.globalData.familyList = [];
          });
      })
      .then(() => {
        this.globalData.authChecked = true;
        if (typeof this._onAuthChecked === 'function') {
          this._onAuthChecked(true);
          this._onAuthChecked = null;
        }
      })
      .catch(() => {
        console.log('[App] 自动登录校验未通过（用户未注册或网络异常）');
        this.globalData.isLoggedIn = false;
        this.globalData.authChecked = true;

        if (typeof this._onAuthChecked === 'function') {
          this._onAuthChecked(false);
          this._onAuthChecked = null;
        }
      });
  },

  /**
   * 清理用户数据（401 等鉴权失败时由 request.js 调用）
   */
  clearUserData() {
    this.globalData.currentUser = null;
    this.globalData.familyList = [];
    this.globalData.currentFamilyId = null;
    this.globalData.isLoggedIn = false;
    this.globalData.authChecked = false;
    console.log('[App] 用户数据已清理');
  },

  /**
   * 全局状态
   *
   * currentUser      当前登录用户信息（id、nickname、avatarUrl、status）
   * familyList       用户已加入的家庭列表，首版始终为空数组，家庭模块开发后填充
   * currentFamilyId  当前激活的家庭 ID，多家庭时用于切换（首版为 null）
   * isLoggedIn       是否已登录
   * authChecked      自动登录校验是否已完成（用于登录页等待判断）
   */
  globalData: {
    currentUser: null,
    familyList: [],
    currentFamilyId: null,
    isLoggedIn: false,
    authChecked: false
  }
});
