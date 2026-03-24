// pages/protocol/protocol.js
const staticApi = require('../../utils/staticApi.js');

Page({
  data: {
    title: '',
    content: '',
    loading: true,
    loadFailed: false,
    fileType: '',
    statusBarHeight: 0
  },

  onLoad(options) {
    const { type } = options;
    if (!type) {
      console.error('[Protocol] 缺少 type 参数');
      this.setData({ loading: false, loadFailed: true });
      return;
    }

    wx.getSystemInfo({
      success: (res) => {
        this.setData({ statusBarHeight: res.statusBarHeight });
      }
    });

    this.setData({ fileType: type });
    this._loadContent(type);
  },

  _loadContent(type) {
    this.setData({ loading: true, loadFailed: false });

    staticApi.getProtocolFile(type)
      .then(res => {
        this.setData({
          title: res.title,
          content: res.content,
          loading: false
        });
        wx.setNavigationBarTitle({ title: res.title });
      })
      .catch(err => {
        console.error('[Protocol] 加载协议内容失败:', err);
        this.setData({ loading: false, loadFailed: true });
      });
  },

  onRetry() {
    this._loadContent(this.data.fileType);
  },

  onBack() {
    wx.navigateBack({ delta: 1 });
  }
});
