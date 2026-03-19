// pages/event/list/list.js
const eventApi = require('../../../utils/eventApi.js');
const familyApi = require('../../../utils/familyApi.js');

function pad2(n) {
  const v = Number(n);
  if (!v) return '';
  return v < 10 ? '0' + v : '' + v;
}

function categoryMeta(category) {
  const map = {
    BIRTHDAY:        { emoji: '🎂', tagBg: '#ffe0eb', tagColor: '#e85577', label: '生日',  iconBg: 'linear-gradient(135deg, #ff6b9d 0%, #ff4b7d 100%)' },
    ANNIVERSARY:     { emoji: '💍', tagBg: '#ede5ff', tagColor: '#8b6fe8', label: '纪念日', iconBg: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)' },
    HOLIDAY:         { emoji: '🎉', tagBg: '#deeeff', tagColor: '#4a90d9', label: '节假日', iconBg: 'linear-gradient(135deg, #4facfe 0%, #00c9ff 100%)' },
    DOCUMENT_EXPIRY: { emoji: '🪪', tagBg: '#fff3dc', tagColor: '#d4850a', label: '证件',   iconBg: 'linear-gradient(135deg, #f7971e 0%, #ffd200 100%)' },
    HEALTH:          { emoji: '🩺', tagBg: '#ddf5ea', tagColor: '#2db56d', label: '健康',   iconBg: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
    PAYMENT:         { emoji: '💳', tagBg: '#ffe5e5', tagColor: '#e85555', label: '缴费',   iconBg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
    OTHER:           { emoji: '📌', tagBg: '#e5ebf2', tagColor: '#6b8aa0', label: '其他',   iconBg: 'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)' }
  };
  return map[category] || map.OTHER;
}

Page({
  data: {
    loading: true,
    refreshing: false,

    familyId: null,
    currentFamilyName: '',

    list: [],
    filteredList: [],

    keyword: '',
    activeCategory: 'ALL',

    statusBarHeight: 44   // 默认值（px），onLoad 时用真实值覆盖
  },

  onLoad() {
    try {
      const info = wx.getSystemInfoSync();
      this.setData({ statusBarHeight: info.statusBarHeight || 44 });
    } catch (e) {
      // 保留默认值
    }
    this._initFamily().then(() => this._loadList());
  },

  onShow() {
    // 从编辑页返回时刷新
    if (this.data.familyId) {
      this._loadList(true);
    }
  },

  onBack() {
    wx.navigateBack({ delta: 1 });
  },

  onRefresh() {
    this.setData({ refreshing: true });
    this._loadList(true).finally(() => {
      this.setData({ refreshing: false });
    });
  },

  onCreateTap() {
    wx.navigateTo({ url: '/pages/event/edit/edit' });
  },

  onItemTap(e) {
    const eventId = e.currentTarget.dataset.eventId;
    wx.navigateTo({ url: `/pages/event/edit/edit?eventId=${eventId}` });
  },

  onEditTap(e) {
    const eventId = e.currentTarget.dataset.eventId;
    wx.navigateTo({ url: `/pages/event/edit/edit?eventId=${eventId}` });
  },

  onDeleteTap(e) {
    const eventId = e.currentTarget.dataset.eventId;
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
            this._loadList(true);
          })
          .catch(() => {
            wx.hideLoading();
          });
      }
    });
  },

  onKeywordInput(e) {
    const keyword = (e.detail.value || '').trim();
    this.setData({ keyword });
    this._applyFilter();
  },

  onSearchConfirm() {
    this._applyFilter();
  },

  onClearKeyword() {
    this.setData({ keyword: '' });
    this._applyFilter();
  },

  onCategoryTap(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ activeCategory: category });
    this._applyFilter();
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

    // 兜底：拉一次我的家庭，取第一个
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

  _loadList(silent) {
    const familyId = this.data.familyId;
    if (!familyId) return Promise.resolve();

    if (!silent) {
      this.setData({ loading: true });
    }

    return eventApi.listEvents(familyId)
      .then(list => {
        const raw = list || [];
        const normalized = raw.map(item => {
          const meta = categoryMeta(item.category);
          const dateText = `${pad2(item.month)}-${pad2(item.day)}`;
          return Object.assign({}, item, {
            _emoji:         meta.emoji,
            _tagBg:         meta.tagBg,
            _tagColor:      meta.tagColor,
            _iconBg:        meta.iconBg,
            _categoryLabel: meta.label,
            _dateText:      dateText
          });
        });

        this.setData({
          list: normalized,
          loading: false
        });
        this._applyFilter();
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  _applyFilter() {
    const keyword = (this.data.keyword || '').trim().toLowerCase();
    const activeCategory = this.data.activeCategory;

    let list = this.data.list || [];
    if (activeCategory && activeCategory !== 'ALL') {
      list = list.filter(v => v.category === activeCategory);
    }
    if (keyword) {
      list = list.filter(v => {
        const t = (v.title || '').toLowerCase();
        const c = (v.creatorNickname || '').toLowerCase();
        return t.includes(keyword) || c.includes(keyword);
      });
    }
    this.setData({ filteredList: list });
  }
});

