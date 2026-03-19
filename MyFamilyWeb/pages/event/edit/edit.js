// pages/event/edit/edit.js
const eventApi = require('../../../utils/eventApi.js');
const familyApi = require('../../../utils/familyApi.js');

function clampInt(v, min, max, fallback) {
  const n = parseInt(v, 10);
  if (isNaN(n)) return fallback;
  return Math.max(min, Math.min(max, n));
}

function safeDateText(month, day) {
  const m = clampInt(month, 1, 12, 1);
  const d = clampInt(day, 1, 31, 1);
  return `${m}月${d}日`;
}

Page({
  data: {
    submitting: false,
    isEdit: false,

    familyId: null,
    currentFamilyName: '',

    eventId: null,
    detail: {},

    categoryOptions: [
      { value: 'BIRTHDAY', label: '生日' },
      { value: 'ANNIVERSARY', label: '纪念日' },
      { value: 'HOLIDAY', label: '节假日' },
      { value: 'DOCUMENT_EXPIRY', label: '证件到期' },
      { value: 'HEALTH', label: '健康体检' },
      { value: 'PAYMENT', label: '还款缴费' },
      { value: 'OTHER', label: '其他' }
    ],
    categoryIndex: 0,

    monthOptions: Array.from({ length: 12 }).map((_, i) => i + 1),
    dayOptions: Array.from({ length: 31 }).map((_, i) => i + 1),
    monthIndex: 0,
    dayIndex: 0,

    advanceOptions: [0, 1, 2, 3, 5, 7, 10, 15, 30],
    advanceIndex: 0,

    form: {
      title: '',
      category: 'BIRTHDAY',
      dateType: 0,
      month: 1,
      day: 1,
      description: '',
      remindEnabled: 0,
      remindAdvanceDays: 0,
      remindTarget: 'ALL',
      remindUserId: null
    },

    statusBarHeight: 44
  },

  onLoad(options) {
    try {
      const info = wx.getSystemInfoSync();
      this.setData({ statusBarHeight: info.statusBarHeight || 44 });
    } catch (e) {
      // 保留默认值
    }

    const eventId = options && options.eventId ? Number(options.eventId) : null;
    this.setData({
      eventId: eventId,
      isEdit: !!eventId
    });

    this._initFamily()
      .then(() => {
        if (eventId) {
          return this._loadDetail(eventId);
        }
      });
  },

  onBack() {
    wx.navigateBack({ delta: 1 });
  },

  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value });
  },

  onDescInput(e) {
    this.setData({ 'form.description': e.detail.value });
  },

  onCategoryChange(e) {
    const idx = Number(e.detail.value || 0);
    const opt = this.data.categoryOptions[idx];
    this.setData({
      categoryIndex: idx,
      'form.category': opt.value
    });
  },

  onDateTypeTap(e) {
    const type = Number(e.currentTarget.dataset.type);
    this.setData({ 'form.dateType': type });
  },

  onMonthChange(e) {
    const idx = Number(e.detail.value || 0);
    const month = this.data.monthOptions[idx];
    this.setData({
      monthIndex: idx,
      'form.month': month
    });
  },

  onDayChange(e) {
    const idx = Number(e.detail.value || 0);
    const day = this.data.dayOptions[idx];
    this.setData({
      dayIndex: idx,
      'form.day': day
    });
  },

  onRemindSwitch(e) {
    const on = !!e.detail.value;
    this.setData({ 'form.remindEnabled': on ? 1 : 0 });
  },

  onAdvanceChange(e) {
    const idx = Number(e.detail.value || 0);
    const days = this.data.advanceOptions[idx];
    this.setData({
      advanceIndex: idx,
      'form.remindAdvanceDays': days
    });
  },

  onTargetTap(e) {
    const target = e.currentTarget.dataset.target;
    this.setData({ 'form.remindTarget': target });
  },

  onDelete() {
    const eventId = this.data.eventId;
    if (!eventId) return;

    wx.showModal({
      title: '确认删除',
      content: '删除后不可恢复（仅创建人可删除）',
      confirmColor: '#ff4b6e',
      success: res => {
        if (!res.confirm) return;
        wx.showLoading({ title: '删除中...' });
        eventApi.deleteEvent(eventId)
          .then(() => {
            wx.hideLoading();
            wx.showToast({ title: '已删除', icon: 'success', duration: 1200 });
            wx.navigateBack({ delta: 1 });
          })
          .catch(() => {
            wx.hideLoading();
          });
      }
    });
  },

  onSubmit() {
    if (this.data.submitting) return;

    const form = this.data.form;
    const familyId = this.data.familyId;
    if (!familyId) {
      wx.showToast({ title: '未选择家庭', icon: 'none' });
      return;
    }
    if (!form.title || !form.title.trim()) {
      wx.showToast({ title: '请填写事项名称', icon: 'none' });
      return;
    }

    const payload = Object.assign({}, form, {
      familyId: familyId,
      title: form.title.trim(),
      month: clampInt(form.month, 1, 12, 1),
      day: clampInt(form.day, 1, 31, 1),
      dateType: form.dateType === 1 ? 1 : 0,
      remindEnabled: form.remindEnabled === 1 ? 1 : 0,
      remindAdvanceDays: clampInt(form.remindAdvanceDays, 0, 365, 0),
      remindTarget: form.remindTarget || 'ALL'
    });

    this.setData({ submitting: true });

    if (!this.data.isEdit) {
      wx.showLoading({ title: '创建中...' });
      eventApi.createEvent(payload)
        .then(() => {
          wx.hideLoading();
          wx.showToast({ title: '创建成功', icon: 'success', duration: 1200 });
          wx.navigateBack({ delta: 1 });
        })
        .catch(() => {
          wx.hideLoading();
        })
        .finally(() => this.setData({ submitting: false }));
      return;
    }

    // 编辑：后端当前未提供 update 接口，先提示并预留调用
    wx.showModal({
      title: '暂不支持保存修改',
      content: '当前后端仅开放创建/查询/删除接口。编辑页面已就绪，待后端补充 /api/event/update 后即可直接启用保存。',
      confirmText: '我知道了',
      showCancel: false,
      success: () => {
        this.setData({ submitting: false });
      }
    });
  },

  _initFamily() {
    const app = getApp();
    const cachedFamilyId = app.globalData.currentFamilyId;

    if (cachedFamilyId) {
      this.setData({ familyId: cachedFamilyId });
      const cachedList = app.globalData.familyList || [];
      const current = cachedList.find(f => f.familyId === cachedFamilyId);
      if (current) {
        this.setData({ currentFamilyName: current.familyName || '' });
      }
      return Promise.resolve();
    }

    return familyApi.getMyFamilies().then(list => {
      const familyList = list || [];
      app.globalData.familyList = familyList;
      if (familyList.length === 0) {
        wx.redirectTo({ url: '/pages/family/guide/guide' });
        return;
      }
      const current = familyList[0];
      app.globalData.currentFamilyId = current.familyId;
      this.setData({
        familyId: current.familyId,
        currentFamilyName: current.familyName || ''
      });
    });
  },

  _loadDetail(eventId) {
    wx.showLoading({ title: '加载中...' });
    return eventApi.getEventDetail(eventId)
      .then(detail => {
        wx.hideLoading();
        const d = detail || {};

        const catIdx = Math.max(0, this.data.categoryOptions.findIndex(v => v.value === d.category));
        const month = clampInt(d.month, 1, 12, 1);
        const day = clampInt(d.day, 1, 31, 1);
        const monthIndex = Math.max(0, month - 1);
        const dayIndex = Math.max(0, day - 1);

        const advanceDays = clampInt(d.remindAdvanceDays, 0, 365, 0);
        let advIdx = this.data.advanceOptions.findIndex(v => v === advanceDays);
        if (advIdx < 0) advIdx = 0;

        this.setData({
          detail: d,
          categoryIndex: catIdx,
          monthIndex,
          dayIndex,
          advanceIndex: advIdx,
          form: {
            title: d.title || '',
            category: d.category || this.data.categoryOptions[0].value,
            dateType: d.dateType === 1 ? 1 : 0,
            month,
            day,
            description: d.description || '',
            remindEnabled: d.remindEnabled === 1 ? 1 : 0,
            remindAdvanceDays: advanceDays,
            remindTarget: d.remindTarget || 'ALL',
            remindUserId: d.remindUserId || null
          }
        });

        wx.setNavigationBarTitle({
          title: safeDateText(month, day)
        });
      })
      .catch(() => {
        wx.hideLoading();
      });
  }
});

