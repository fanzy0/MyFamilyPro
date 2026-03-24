/**
 * 用户模块相关 API
 * 基于 request.js 业务层封装，统一处理请求错误
 */

const { get, post } = require('./request.js');

/**
 * 用户登录
 * openid 由微信云托管平台自动注入到 X-WX-OPENID Header，无需传参
 * 后端根据 openid 查询或创建用户，返回用户信息和家庭列表
 *
 * @return {Promise<{user: Object, familyList: Array}>} 登录响应
 */
function login() {
  return post('/auth/login');
}

/**
 * 获取当前用户信息
 * 用于 app.onLaunch 中校验用户是否已在系统中注册，同时获取最新用户信息
 * X-WX-OPENID 由云托管自动携带，无需前端传递
 *
 * @return {Promise<{id, nickname, avatarUrl, status}>} 用户信息
 */
function getMe() {
  return get('/user/me');
}

/**
 * 更新用户基础资料
 * 用户在个人资料页通过 type="nickname" 或 open-type="chooseAvatar" 设置后调用
 * nickname 和 avatarUrl 均为可选，仅更新非空字段
 *
 * @param {Object} profileData 资料数据
 * @param {String} [profileData.nickname] 昵称
 * @param {String} [profileData.avatarUrl] 头像地址
 * @return {Promise<{id, nickname, avatarUrl, status}>} 更新后的用户信息
 */
function updateProfile(profileData) {
  return post('/user/updateProfile', profileData);
}

/**
 * 注销当前用户账号
 * 调用后用户状态变为禁用，无法再次登录
 * 前端应在成功回调中清除本地数据并跳转登录页
 *
 * @return {Promise<void>}
 */
function deactivateAccount() {
  return post('/user/deactivate');
}

module.exports = {
  login,
  getMe,
  updateProfile,
  deactivateAccount
};
