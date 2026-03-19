// pages/index/index.js
const api = require('../../utils/api.js');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    togetherDays: 0, // 一起走过的天数
    memoryImages: [], // 回忆轮播图片
    startDate: '2016-02-19', // 开始日期
    currentTab: 0 // 当前选中的tab，0:首页 1:我的
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    if (!this._checkLogin()) return;
    this.calculateDays();
    // 加载回忆图片
    this.loadMemoryImages();
  },

  /**
   * 登录守卫：校验用户是否已登录
   * 若 authChecked 为 true 且 isLoggedIn 为 false，跳转登录页
   * @return {Boolean} 是否已登录
   */
  _checkLogin() {
    const app = getApp();
    if (app.globalData.authChecked && !app.globalData.isLoggedIn) {
      console.log('[Index] 用户未登录，跳转登录页');
      wx.redirectTo({ url: '/pages/login/login' });
      return false;
    }
    return true;
  },

  /**
   * 计算从2016年2月19日到今天的天数
   */
  calculateDays() {
    const startDate = new Date(this.data.startDate);
    const today = new Date();
    
    // 计算毫秒差
    const diffTime = Math.abs(today - startDate);
    // 转换为天数
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    this.setData({
      togetherDays: diffDays
    });
  },

  /**
   * 加载回忆图片
   * 使用微信云托管调用后端接口
   */
  loadMemoryImages() {
    console.log('开始加载轮播图片');
    
    // 显示加载提示
    wx.showLoading({
      title: '加载中...'
    });

    // 调用API获取轮播图列表
    api.getBannerList()
      .then((bannerList) => {
        console.log('获取轮播图列表成功，数量:', bannerList.length);
        if (bannerList && bannerList.length > 0) {
          // 加载图片
          return this.loadImagesByList(bannerList);
        } else {
          // 没有图片数据
          this.setData({
            memoryImages: []
          });
          wx.hideLoading();
        }
      })
      .catch((err) => {
        console.error('获取轮播图列表失败:', err);
        wx.hideLoading();
        wx.showToast({
          title: err.message || '加载失败',
          icon: 'none',
          duration: 3000
        });
      });
  },

  /**
   * 根据图片列表构建图片URL数组
   * 使用云托管的方式获取图片
   * @param {Array} bannerList 轮播图列表
   */
  loadImagesByList(bannerList) {
    console.log('开始加载图片，数量:', bannerList.length);
    const imagePromises = [];

    // 遍历每个图片，通过云托管获取图片
    bannerList.forEach((banner, index) => {
      if (banner.imagePath) {
        const promise = this.loadSingleImage(banner.imagePath, index);
        imagePromises.push(promise);
      }
    });

    // 等待所有图片加载完成
    return Promise.all(imagePromises).then((images) => {
      // 过滤掉加载失败的图片（null值）
      const validImages = images.filter(img => img !== null);
      this.setData({
        memoryImages: validImages
      });
      wx.hideLoading();
      console.log('图片加载完成，成功加载数量:', validImages.length);
      
      if (validImages.length === 0) {
        wx.showToast({
          title: '暂无图片',
          icon: 'none'
        });
      }
    }).catch((err) => {
      console.error('图片加载失败:', err);
      wx.hideLoading();
      wx.showToast({
        title: '图片加载失败',
        icon: 'none'
      });
    });
  },

  /**
   * 加载单张图片
   * @param {String} imagePath 图片相对路径
   * @param {Number} index 图片索引
   * @return {Promise} 返回图片的Base64数据URL
   */
  loadSingleImage(imagePath, index) {
    return api.getBannerImage(imagePath)
      .then((arrayBuffer) => {
        // 将ArrayBuffer转换为Base64
        const base64 = wx.arrayBufferToBase64(arrayBuffer);
        const imageUrl = `data:image/jpeg;base64,${base64}`;
        console.log(`图片${index}加载成功`);
        return imageUrl;
      })
      .catch((err) => {
        console.error(`图片${index}加载失败:`, err);
        return null;
      });
  },

  /**
   * 导航到热干之手页面
   */
  navigateToLove() {
    wx.showToast({
      title: '执子之手功能开发中',
      icon: 'none'
    });
    // wx.navigateTo({
    //   url: '/pages/love/love'
    // });
  },

  /**
   * 导航到平安喜乐页面
   */
  navigateToHome() {
    wx.showToast({
      title: '平安喜乐功能开发中',
      icon: 'none'
    });
    // wx.navigateTo({
    //   url: '/pages/home/home'
    // });
  },

  /**
   * 导航到重要事项页面
   */
  navigateToImportant() {
    wx.navigateTo({ url: '/pages/event/list/list' });
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 每次显示页面时重新计算天数
    this.calculateDays();
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.calculateDays();
    this.loadMemoryImages();
    wx.stopPullDownRefresh();
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    return {
      title: '我们的家 - 记录每一个温馨时刻',
      path: '/pages/index/index'
    };
  },

  /**
   * 切换底部Tab
   */
  switchTab(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      currentTab: index
    });
    
    if (index === 1) {
      wx.showToast({
        title: '我的页面开发中',
        icon: 'none'
      });
      // 后续可以跳转到我的页面
      // wx.navigateTo({
      //   url: '/pages/my/my'
      // });
    }
  }
});

