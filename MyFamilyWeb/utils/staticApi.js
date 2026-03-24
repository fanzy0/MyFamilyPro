/**
 * 静态协议文件相关 API
 * 无需登录即可访问，用于展示用户协议和隐私声明
 */

const { get } = require('./request.js');

/**
 * 获取协议文件内容
 * 对应后端 GET /api/static/file?type={type}
 *
 * @param {string} type 文件类型：USER_PROTOCOL | PRIVACY_STATEMENT
 * @return {Promise<{title: string, content: string}>} 协议标题和内容
 */
function getProtocolFile(type) {
  return get('/static/file', { type });
}

module.exports = {
  getProtocolFile
};
