/**
 * Banner（轮播图）相关API接口
 */

const { get, getImage } = require('../utils/request.js');

/**
 * 获取轮播图列表
 * @return {Promise<Array>} 轮播图列表
 */
function getBannerList() {
  return get('/banner/list');
}

/**
 * 获取图片（返回ArrayBuffer）
 * @param {String} imagePath 图片相对路径
 * @return {Promise<ArrayBuffer>} 图片数据
 */
function getBannerImage(imagePath) {
  return getImage(`/banner/image/view?path=${encodeURIComponent(imagePath)}`);
}

/**
 * 保存轮播图
 * @param {Object} bannerData 轮播图数据
 * @return {Promise}
 */
function saveBanner(bannerData) {
  const { post } = require('../utils/request.js');
  return post('/banner/save', bannerData);
}

/**
 * 删除轮播图
 * @param {Number} bannerId 轮播图ID
 * @return {Promise}
 */
function deleteBanner(bannerId) {
  const { post } = require('../utils/request.js');
  return post('/banner/delete', { bannerId });
}

module.exports = {
  getBannerList,
  getBannerImage,
  saveBanner,
  deleteBanner
};
