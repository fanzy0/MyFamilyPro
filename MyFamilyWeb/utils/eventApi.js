/**
 * Event（家庭重要事项）相关API接口
 * 对应后端：/api/event/**
 */

const { get, post, del } = require('../utils/request.js');

/**
 * 查询家庭事项列表
 * GET /api/event/list?familyId=xxx
 */
function listEvents(familyId) {
  return get('/event/list', { familyId });
}

/**
 * 查询事项详情
 * GET /api/event/detail?eventId=xxx
 */
function getEventDetail(eventId) {
  return get('/event/detail', { eventId });
}

/**
 * 创建事项
 * POST /api/event/create
 */
function createEvent(data) {
  return post('/event/create', data);
}

/**
 * 删除事项（仅创建人）
 * DELETE /api/event/delete?eventId=xxx
 */
function deleteEvent(eventId) {
  return del('/event/delete', { eventId });
}

/**
 * 修改事项（当前后端未提供接口，前端先预留）
 * 建议后端补充：POST /api/event/update
 */
function updateEvent(data) {
  return post('/event/update', data);
}

module.exports = {
  listEvents,
  getEventDetail,
  createEvent,
  deleteEvent,
  updateEvent
};

