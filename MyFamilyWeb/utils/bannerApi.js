/**
 * 轮播图/评论模块 API
 * 对应后端 /api/banner/* 接口
 */

const { get, post, getImage } = require('./request.js');

/**
 * 查询指定家庭下启用的轮播图片列表
 *
 * @param {Number} familyId 家庭ID
 * @return {Promise<Array<{bannerId, imagePath, position, title, description, linkUrl, status}>>}
 */
function getBannerList(familyId) {
  return get('/banner/list', { familyId });
}

/**
 * 按图片相对路径获取轮播图片字节流（ArrayBuffer）
 * 仅用于旧版后端存储的图片（imagePath 为数字ID字符串）
 *
 * @param {String} imagePath 图片标识（t_banner.image_path）
 * @return {Promise<ArrayBuffer>}
 */
function getBannerImage(imagePath) {
  return getImage(`/banner/image/view?path=${encodeURIComponent(imagePath)}`);
}

/**
 * 保存轮播图片记录
 * 图片已通过 wx.cloud.uploadFile 上传至云存储，此处仅保存 fileID 映射
 *
 * @param {{familyId: number, imagePath: string, title?: string, description?: string}} data
 * @return {Promise<string>} 操作结果消息
 */
function saveBanner(data) {
  return post('/banner/save', data);
}

/**
 * 删除轮播图片
 *
 * @param {number} bannerId 轮播图业务ID
 * @return {Promise<string>} 操作结果消息
 */
function deleteBanner(bannerId) {
  return post(`/banner/delete?bannerId=${bannerId}`);
}

module.exports = {
  getBannerList,
  getBannerImage,
  saveBanner,
  deleteBanner
};

