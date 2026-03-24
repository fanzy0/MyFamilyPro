/**
 * 业务层请求封装
 * 在 cloudRequest.js 底层封装基础上增加统一错误处理：
 *   - 401：身份验证失败，清理用户数据并跳转登录页（非登录页才跳转）
 *   - 403：账号被禁用，toast 提示
 *   - 5xx / 网络错误：toast 提示"服务器异常"
 *   - 其他：toast 提示具体错误信息
 */

const cloudRequest = require('./cloudRequest.js');

/**
 * GET 请求（业务层）
 * @param {String} path 请求路径（如：/user/me）
 * @param {Object} params 查询参数（可选）
 * @return {Promise}
 */
function get(path, params) {
  return cloudRequest.get(path, params).catch(handleError);
}

/**
 * POST 请求（业务层）
 * @param {String} path 请求路径（如：/auth/login）
 * @param {Object} data 请求体数据（可选）
 * @return {Promise}
 */
function post(path, data) {
  return cloudRequest.post(path, data).catch(handleError);
}

/**
 * DELETE 请求（业务层）
 * @param {String} path 请求路径（如：/event/delete）
 * @param {Object} params 查询参数（可选）
 * @return {Promise}
 */
function del(path, params) {
  return cloudRequest.del(path, params).catch(handleError);
}

/**
 * 获取图片（返回 ArrayBuffer）
 * @param {String} path 请求路径
 * @return {Promise<ArrayBuffer>}
 */
function getImage(path) {
  return cloudRequest.getImage(path).catch(handleError);
}

/**
 * 统一错误处理
 * @param {Object} err 错误对象，包含 code 和 message
 * @return {Promise} 继续向上抛出错误，让业务调用方可以继续处理
 */
function handleError(err) {
  const code = err && err.code;

  if (code === 401) {
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    const isOnLoginPage = currentPage && currentPage.route && currentPage.route.includes('login');

    if (!isOnLoginPage) {
      const app = getApp();
      if (app && typeof app.clearUserData === 'function') {
        app.clearUserData();
      }
      wx.redirectTo({ url: '/pages/login/login' });
    }
  } else if (code === 403) {
    wx.showToast({
      title: '账号已被禁用，请重新授权登录',
      icon: 'none',
      duration: 3000
    });
  } else if (code >= 500) {
    wx.showToast({
      title: '服务器异常，请稍后重试',
      icon: 'none',
      duration: 2000
    });
  } else if (code === -1) {
    wx.showToast({
      title: '网络请求失败，请检查网络',
      icon: 'none',
      duration: 2000
    });
  } else if (code !== 401) {
    wx.showToast({
      title: (err && err.message) || '请求失败，请稍后重试',
      icon: 'none',
      duration: 2000
    });
  }

  return Promise.reject(err);
}

module.exports = {
  get,
  post,
  del,
  getImage
};
