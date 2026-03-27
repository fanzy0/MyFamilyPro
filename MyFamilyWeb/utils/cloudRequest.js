/**
 * 云托管网络请求封装
 * 统一管理云托管的HTTP请求
 */

const config = require('./config.js');

/**
 * 云托管请求方法
 * @param {Object} options 请求配置
 * @param {String} options.path 请求路径（如：/banner/list）
 * @param {String} options.method 请求方法（GET、POST等）
 * @param {Object} options.data 请求数据
 * @param {Object} options.header 请求头（可选）
 * @param {String} options.dataType 返回数据类型（json、other等）
 * @param {String} options.responseType 响应类型（text、arraybuffer等）
 * @return {Promise} 返回Promise对象
 */
function cloudRequest(options) {
  return new Promise((resolve, reject) => {
    // 构建完整路径
    const fullPath = `${config.apiBasePath}${options.path}`;
    
    // 默认请求头
    const defaultHeader = {
      'X-WX-SERVICE': config.serviceName,
      'content-type': 'application/json'
    };

    // 临时登录模式：大多数请求自动携带 TEMP-OPENID，确保后续接口也以临时账号身份鉴权
    // 但真实登录接口 /auth/login 不能带该头，否则会污染微信一键登录流程
    const app = getApp();
    const shouldAttachTempOpenId = options.attachTempOpenId !== false;
    if (app && app.globalData && app.globalData.isTempLogin && shouldAttachTempOpenId) {
      defaultHeader['TEMP-OPENID'] = 'QINGJUXUNUAN001';
    }

    // 合并请求头（options.header 可覆盖 defaultHeader）
    const header = Object.assign({}, defaultHeader, options.header || {});

    console.log(`[CloudRequest] 开始请求: ${options.method} ${fullPath}`);

    wx.cloud.callContainer({
      config: {
        env: config.cloudEnv
      },
      path: fullPath,
      method: options.method || 'GET',
      data: options.data || '',
      header: header,
      dataType: options.dataType || 'json',
      responseType: options.responseType || 'text',
      success: (res) => {
        console.log(`[CloudRequest] 请求成功: ${fullPath}, 状态码: ${res.statusCode}`);
        if (res.statusCode === 200) {
          resolve(res.data);
        } else {
          console.error(`[CloudRequest] 请求失败: ${fullPath}, 状态码: ${res.statusCode}`);
          reject({
            code: res.statusCode,
            message: '请求失败',
            data: res.data,
            path: options.path
          });
        }
      },
      fail: (err) => {
        console.error(`[CloudRequest] 请求异常: ${fullPath}`, err);
        reject({
          code: -1,
          message: err.errMsg || '网络请求失败',
          error: err,
          path: options.path
        });
      }
    });
  });
}

/**
 * GET请求
 * @param {String} path 请求路径
 * @param {Object} params 查询参数（可选）
 * @return {Promise}
 */
function get(path, params) {
  // 将参数拼接到URL中
  if (params && Object.keys(params).length > 0) {
    const query = Object.keys(params)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
      .join('&');
    path = `${path}?${query}`;
  }

  return cloudRequest({
    path: path,
    method: 'GET'
  });
}

/**
 * POST请求
 * @param {String} path 请求路径
 * @param {Object} data 请求数据
 * @param {Object} header 附加请求头（可选）
 * @return {Promise}
 */
function post(path, data, header) {
  // 真实微信登录必须使用当前微信 openid，不可附带临时账号头
  const attachTempOpenId = path !== '/auth/login';
  return cloudRequest({
    path: path,
    method: 'POST',
    data: data,
    header: header,
    attachTempOpenId: attachTempOpenId
  });
}

/**
 * DELETE请求
 * @param {String} path 请求路径
 * @param {Object} params 查询参数（可选）
 * @return {Promise}
 */
function del(path, params) {
  // 将参数拼接到URL中
  if (params && Object.keys(params).length > 0) {
    const query = Object.keys(params)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
      .join('&');
    path = `${path}?${query}`;
  }

  return cloudRequest({
    path: path,
    method: 'DELETE'
  });
}

/**
 * 获取图片（返回ArrayBuffer）
 * @param {String} path 请求路径
 * @return {Promise}
 */
function getImage(path) {
  return cloudRequest({
    path: path,
    method: 'GET',
    dataType: 'other',
    responseType: 'arraybuffer'
  });
}

module.exports = {
  cloudRequest,
  get,
  post,
  del,
  getImage
};
