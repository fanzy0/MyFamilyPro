// app.js
App({
  onLaunch() {
    // 初始化云开发环境
    if (!wx.cloud) {
      console.error('请使用 2.2.3 或以上的基础库以使用云能力');
    } else {
      wx.cloud.init({
        // 云开发环境ID
        env: 'prod-8gq9rikccd2b0066',
        traceUser: true
      });
      console.log('云开发环境初始化成功');
    }
  }
})
