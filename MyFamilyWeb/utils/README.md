# 微信云托管集成说明

## 修改内容

### 1. 初始化云开发环境（app.js）
在 `app.js` 中添加云开发初始化代码：
```javascript
wx.cloud.init({
  env: 'prod-8gq9rikccd2b0066',
  traceUser: true
});
```

### 2. 创建工具类（utils目录）

#### utils/config.js
- 统一管理云开发环境ID和服务名称
- 配置项：
  - `cloudEnv`: 云开发环境ID
  - `serviceName`: 云托管服务名称
  - `apiBasePath`: API基础路径

#### utils/cloudRequest.js
- 封装云托管的HTTP请求
- 提供方法：
  - `cloudRequest(options)`: 通用请求方法
  - `get(path, params)`: GET请求
  - `post(path, data)`: POST请求
  - `getImage(path)`: 获取图片（返回ArrayBuffer）

#### utils/api.js
- Banner相关API接口封装
- 提供方法：
  - `getBannerList()`: 获取轮播图列表
  - `getBannerImage(imagePath)`: 获取图片
  - `saveBanner(bannerData)`: 保存轮播图
  - `deleteBanner(bannerId)`: 删除轮播图

### 3. 修改页面代码（pages/index/index.js）

#### 主要改动：
1. 移除硬编码的API地址配置
2. 引入API模块：`const api = require('../../utils/api.js')`
3. 使用Promise方式调用API
4. 添加加载提示（wx.showLoading/wx.hideLoading）
5. 优化错误处理

#### 调用示例：
```javascript
// 获取轮播图列表
api.getBannerList()
  .then((bannerList) => {
    console.log('获取成功', bannerList);
  })
  .catch((err) => {
    console.error('获取失败', err);
  });

// 获取图片
api.getBannerImage(imagePath)
  .then((arrayBuffer) => {
    const base64 = wx.arrayBufferToBase64(arrayBuffer);
    const imageUrl = `data:image/jpeg;base64,${base64}`;
  });
```

## 文件结构

```
MyFamilyWeb/
├── app.js                          # 云开发初始化
├── utils/                          # 工具类目录（新增）
│   ├── config.js                   # 云托管配置
│   ├── cloudRequest.js             # 云托管请求封装
│   └── api.js                      # API接口封装
└── pages/
    └── index/
        └── index.js                # 使用新的API调用方式
```

## 配置说明

如果需要修改云开发环境或服务名称，只需修改 `utils/config.js` 文件：

```javascript
module.exports = {
  cloudEnv: 'prod-8gq9rikccd2b0066',    // 你的云开发环境ID
  serviceName: 'my-test',                // 你的云托管服务名称
  apiBasePath: '/api'
};
```

## 优势

1. **代码复用**：统一的请求封装，避免重复代码
2. **易于维护**：配置集中管理，修改方便
3. **类型安全**：Promise方式调用，支持async/await
4. **错误处理**：统一的错误处理机制
5. **日志完善**：完整的请求日志，便于调试

## 注意事项

1. 确保小程序已开通云开发功能
2. 确保云托管服务已部署并正常运行
3. 在微信开发者工具中测试时，需要开启"不校验合法域名"选项
4. 正式发布前，需要在小程序后台配置云托管域名白名单

## 测试步骤

1. 在微信开发者工具中打开项目
2. 点击"编译"运行小程序
3. 查看控制台日志，确认云托管请求成功
4. 检查页面是否正常显示轮播图片

## 调试技巧

- 查看控制台日志：`[CloudRequest]` 开头的日志
- 检查网络请求：在开发者工具的Network面板查看
- 如果请求失败，检查：
  - 云开发环境ID是否正确
  - 云托管服务是否正常运行
  - 后端API路径是否正确
