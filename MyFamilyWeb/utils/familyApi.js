/**
 * 家庭模块相关 API
 * 基于 request.js 业务层封装，对应后端 /api/family/* 接口
 */

const { get, post } = require('./request.js');

/**
 * 创建家庭
 * 当前用户自动成为户主，familyCode 由后端生成
 *
 * @param {String} familyName 家庭名称（2~64字符）
 * @param {String} [meetDate]  相识相遇日期（可选，格式 YYYY-MM-DD），用于首页展示"一起走过的天数"
 * @return {Promise<{familyId, familyName, familyCode, role}>}
 */
function createFamily(familyName, meetDate) {
  const data = { familyName };
  if (meetDate) data.meetDate = meetDate;
  return post('/family/create', data);
}

/**
 * 根据家庭编号查询家庭信息（加入前预览）
 *
 * @param {String} familyCode 家庭编号
 * @return {Promise<{familyId, familyName, memberCount}>}
 */
function getFamilyByCode(familyCode) {
  return get('/family/by-code', { familyCode });
}

/**
 * 申请加入家庭
 *
 * @param {String} familyCode 家庭编号
 * @return {Promise}
 */
function joinFamily(familyCode) {
  return post('/family/join', { familyCode });
}

/**
 * 查询当前用户已加入（APPROVED）的家庭列表
 *
 * @return {Promise<Array<{familyId, familyName, familyCode, role, memberCount}>>}
 */
function getMyFamilies() {
  return get('/family/my-families');
}

/**
 * 查询家庭详情（名称、编号、成员列表等）
 *
 * @param {Number} familyId 家庭ID
 * @return {Promise<{familyId, familyName, familyCode, memberCount, currentUserRole, members: Array<{userId, nickname, role}>}>}
 */
function getFamilyDetail(familyId) {
  return get('/family/detail', { familyId });
}

/**
 * 更新家庭基础信息（仅户主）
 *
 * @param {{familyId: number, familyName: string, meetDate?: string}} data
 * @return {Promise<string>}
 */
function updateFamilyInfo(data) {
  return post('/family/update', data);
}

/**
 * 户主查询待审批成员列表
 *
 * @param {Number} familyId 家庭ID
 * @return {Promise<Array<{familyMemberId, userId, nickname, avatarUrl, applyTime}>>}
 */
function getPendingMembers(familyId) {
  return get('/family/pending-members', { familyId });
}

/**
 * 户主审批通过
 *
 * @param {Number} familyId 家庭ID
 * @param {Number} familyMemberId 成员记录ID
 * @return {Promise}
 */
function approveJoin(familyId, familyMemberId) {
  return post('/family/approve', { familyId, familyMemberId });
}

/**
 * 户主审批拒绝
 *
 * @param {Number} familyId 家庭ID
 * @param {Number} familyMemberId 成员记录ID
 * @return {Promise}
 */
function rejectJoin(familyId, familyMemberId) {
  return post('/family/reject', { familyId, familyMemberId });
}

module.exports = {
  createFamily,
  getFamilyByCode,
  joinFamily,
  getMyFamilies,
  getFamilyDetail,
  updateFamilyInfo,
  getPendingMembers,
  approveJoin,
  rejectJoin
};
