/**
 * 轮播图/评论模块 API
 * 对应后端 /api/banner/* 接口
 */

const { get, getImage } = require('./request.js');

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
 *
 * @param {String} imagePath 图片相对路径（t_banner.image_path）
 * @return {Promise<ArrayBuffer>}
 */
function getBannerImage(imagePath) {
  return getImage(`/banner/image/view?path=${encodeURIComponent(imagePath)}`);
}

module.exports = {
  getBannerList,
  getBannerImage
};

