// pages/family/guide/guide.js
const familyApi = require('../../../utils/familyApi.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    familyList: [],    // 已加入的家庭列表（APPROVED），非空时展示"进入已有家庭"区块
    loading: false     // 加载家庭列表中
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad() {
    console.log('[Guide] 家庭引导页加载');
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {},

  /**
   * 生命周期函数--监听页面显示
   * 每次显示时重新拉取家庭列表：
   *   - 若用户已有家庭，展示"进入已有家庭"选项
   *   - 若后端返回数据但 globalData 未同步（如从 create/join 页返回），也能刷新
   */
  onShow() {
    this._loadFamilyList();
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
   * 拉取当前用户已加入的家庭列表
   * 优先使用 globalData 缓存，同时异步刷新
   */
  _loadFamilyList() {
    const app = getApp();

    // 先用 globalData 缓存快速渲染，避免白屏
    const cached = app.globalData.familyList || [];
    this.setData({ familyList: cached });

    // 再异步拉取最新数据
    this.setData({ loading: true });
    familyApi.getMyFamilies()
      .then(list => {
        const familyList = list || [];
        app.globalData.familyList = familyList;
        this.setData({ familyList, loading: false });
        console.log('[Guide] 家庭列表刷新完成, count=' + familyList.length);
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  /**
   * 点击「进入」—— 直接进入指定家庭主页
   */
  onEnterFamily(e) {
    const { familyId } = e.currentTarget.dataset;
    const app = getApp();
    app.globalData.currentFamilyId = familyId;
    console.log('[Guide] 进入已有家庭, familyId=' + familyId);
    wx.redirectTo({ url: '/pages/family/home/home' });
  },

  /**
   * 点击「创建我的家庭」，跳转到创建家庭页
   */
  onCreateTap() {
    console.log('[Guide] 跳转到创建家庭页');
    wx.navigateTo({ url: '/pages/family/create/create' });
  },

  /**
   * 点击「加入现有家庭」，跳转到加入家庭页
   */
  onJoinTap() {
    console.log('[Guide] 跳转到加入家庭页');
    wx.navigateTo({ url: '/pages/family/join/join' });
  }
});
