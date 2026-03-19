/**
 * 提醒功能 API 封装
 * 对应后端 /api/remind/** 接口
 */

const request = require('./request.js');

/**
 * 查询当前用户在指定家庭的活跃提醒数量（红点用）
 * GET /api/remind/count?familyId=xxx
 *
 * @param {Number} familyId 家庭ID
 * @return {Promise<Number>}
 */
function getRemindCount(familyId) {
  return request.get('/remind/count', { familyId });
}

/**
 * 查询活跃提醒列表（PENDING + READ），同时后端将 PENDING 标记为 READ
 * GET /api/remind/active?familyId=xxx
 *
 * @param {Number} familyId 家庭ID
 * @return {Promise<Array>} RemindLogVO 列表
 */
function getActiveReminds(familyId) {
  return request.get('/remind/active', { familyId });
}

/**
 * 标记提醒为"已完成"（DONE）
 * POST /api/remind/done
 *
 * @param {Number} remindLogId 提醒记录ID
 * @return {Promise<String>}
 */
function doneRemind(remindLogId) {
  return request.post('/remind/done', { remindLogId });
}

/**
 * 关闭/忽略一条提醒（CLOSED_BY_USER）
 * POST /api/remind/close
 *
 * @param {Number} remindLogId 提醒记录ID
 * @return {Promise<String>}
 */
function closeRemind(remindLogId) {
  return request.post('/remind/close', { remindLogId });
}

/**
 * 查询某事项的历年提醒记录（含所有状态，按年份降序）
 * GET /api/remind/history?eventId=xxx
 *
 * @param {Number} eventId 事项ID
 * @return {Promise<Array>} RemindHistoryVO 列表
 */
function getRemindHistory(eventId) {
  return request.get('/remind/history', { eventId });
}

module.exports = {
  getRemindCount,
  getActiveReminds,
  doneRemind,
  closeRemind,
  getRemindHistory
};
