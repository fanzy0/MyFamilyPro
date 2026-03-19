// pages/family/home/home.js
const familyApi = require('../../../utils/familyApi.js');
const bannerApi = require('../../../utils/bannerApi.js');
const remindApi = require('../../../utils/remindApi.js');

Page({
  /**
   * 页面初始数据
   */
  data: {
    // 家庭数据
    currentFamily: null,    // 当前家庭 {familyId, familyName, familyCode, role, memberCount, meetDate}
    familyList: [],         // 所有已加入家庭列表
    loading: true,          // 页面加载状态
    isOwner: false,         // 是否为户主
    togetherDays: null,     // 一起走过的天数（null 时不展示）

    // 首页 Tab 数据
    // 最近回忆轮播图列表，每项结构：
    // { bannerId, imagePath, title, description, linkUrl, loaded, src }
    memoryImages: [],

    // Tab 导航
    activeTab: 'home',      // 'home' | 'my'

    // 我的 Tab — 展开状态
    showFamilySwitch: false,  // 家庭切换面板是否展开
    showApproval: false,      // 成员审批面板是否展开
    approvLoading: false,     // 审批列表加载状态
    pendingMembers: [],       // 待审批成员列表

    // 提醒相关
    remindCount: 0,           // 活跃提醒数量（红点数字，0 时隐藏悬浮按钮）
    showRemindPanel: false,   // 是否展开提醒浮层
    remindList: [],           // 活跃提醒列表（展开浮层时拉取）
    remindLoading: false      // 浮层内容加载状态
  },

  onLoad() {
    console.log('[Home] 家庭主页加载');
    this._refreshFamilyData();
  },

  onReady() {},

  onShow() {
    this._refreshFamilyData();
  },

  onHide() {},

  onUnload() {},

  onPullDownRefresh() {
    this._refreshFamilyData().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 刷新家庭数据
   * 调用 GET /api/family/my-families，更新 globalData 与页面数据
   */
  _refreshFamilyData() {
    this.setData({ loading: true });
    const app = getApp();

    return familyApi.getMyFamilies()
      .then(list => {
        const familyList = list || [];
        app.globalData.familyList = familyList;
        console.log('[Home] 刷新家庭列表, count=' + familyList.length);

        if (familyList.length === 0) {
          app.globalData.currentFamilyId = null;
          wx.redirectTo({ url: '/pages/family/guide/guide' });
          return;
        }

        let currentFamilyId = app.globalData.currentFamilyId;
        let current = familyList.find(f => f.familyId === currentFamilyId);
        if (!current) {
          current = familyList[0];
          app.globalData.currentFamilyId = current.familyId;
        }

        const togetherDays = this._calcTogetherDays(current.meetDate);
        const isOwner = current.role === 'OWNER';

        this.setData({
          currentFamily: current,
          familyList,
          isOwner,
          togetherDays,
          loading: false,
          // 切换家庭后重置展开状态
          showFamilySwitch: false,
          showApproval: false,
          pendingMembers: [],
          // 家庭切换时重置轮播图数据，等待重新加载
          memoryImages: []
        });

        // 户主：静默预加载待审批数量
        if (isOwner) {
          this._loadPendingMembers(current.familyId, true);
        }

        // 加载当前家庭的轮播图列表
        this._loadMemoryBanners(current.familyId);

        // 加载提醒红点数量（静默，不影响主流程）
        this._loadRemindCount(current.familyId);
      })
      .catch(err => {
        this.setData({ loading: false });
        console.error('[Home] 刷新家庭数据失败:', err);
      });
  },

  /**
   * 根据相识日期计算一起走过的天数
   */
  _calcTogetherDays(meetDate) {
    if (!meetDate) return null;
    try {
      const start = new Date(meetDate);
      if (isNaN(start.getTime())) return null;
      const diff = Math.floor((Date.now() - start.getTime()) / (1000 * 60 * 60 * 24));
      return diff >= 0 ? diff : null;
    } catch (e) {
      return null;
    }
  },

  /**
   * 加载待审批成员列表
   * @param {Number} familyId 家庭ID
   * @param {Boolean} silent 静默模式（仅更新数量，不显示 loading）
   */
  _loadPendingMembers(familyId, silent) {
    if (!silent) {
      this.setData({ approvLoading: true });
    }
    return familyApi.getPendingMembers(familyId)
      .then(members => {
        this.setData({
          pendingMembers: members || [],
          approvLoading: false
        });
      })
      .catch(err => {
        this.setData({ approvLoading: false });
        console.error('[Home] 加载待审批成员失败:', err);
      });
  },

  // ============================
  //   底部 Tab 导航
  // ============================

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === this.data.activeTab) return;
    this.setData({
      activeTab: tab,
      showFamilySwitch: false,
      showApproval: false
    });
  },

  // ============================
  //   我的 Tab 操作
  // ============================

  /**
   * 跳转家庭信息修改页（预留）
   */
  onEditInfo() {
    wx.showToast({ title: '信息修改开发中', icon: 'none' });
    // 后续：wx.navigateTo({ url: '/pages/family/edit/edit' });
  },

  /**
   * 展开/收起 家庭切换列表
   */
  onToggleFamilySwitch() {
    this.setData({
      showFamilySwitch: !this.data.showFamilySwitch,
      showApproval: false
    });
  },

  /**
   * 切换当前家庭
   */
  onSwitchFamily(e) {
    const familyId = e.currentTarget.dataset.familyId;
    if (familyId === this.data.currentFamily.familyId) {
      this.setData({ showFamilySwitch: false });
      return;
    }

    const app = getApp();
    app.globalData.currentFamilyId = familyId;
    const current = this.data.familyList.find(f => f.familyId === familyId);
    const togetherDays = this._calcTogetherDays(current.meetDate);
    const isOwner = current.role === 'OWNER';

    this.setData({
      currentFamily: current,
      isOwner,
      togetherDays,
      showFamilySwitch: false,
      showApproval: false,
      pendingMembers: [],
      memoryImages: []
    });

    if (isOwner) {
      this._loadPendingMembers(familyId, true);
    }

    // 加载新家庭的轮播图
    this._loadMemoryBanners(familyId);

    // 切换家庭后重置提醒状态并重新加载
    this.setData({ remindCount: 0, remindList: [], showRemindPanel: false });
    this._loadRemindCount(familyId);

    wx.showToast({ title: '已切换到 ' + current.familyName, icon: 'success', duration: 1500 });
    console.log('[Home] 切换家庭, familyId=' + familyId);
  },

  /**
   * 加载当前家庭的轮播图列表（仅加载元数据，不拉取图片内容）
   * @param {Number} familyId 家庭ID
   */
  _loadMemoryBanners(familyId) {
    if (!familyId) return;
    return bannerApi.getBannerList(familyId)
      .then(list => {
        const banners = list || [];
        const memoryImages = banners.map(b => ({
          bannerId: b.bannerId,
          imagePath: b.imagePath,
          title: b.title,
          description: b.description,
          linkUrl: b.linkUrl,
          status: b.status,
          loaded: false,
          src: ''
        }));
        this.setData({ memoryImages });
        // 预加载第一张，避免首屏空白
        if (memoryImages.length > 0) {
          this._ensureBannerImageLoaded(0);
        }
      })
      .catch(err => {
        console.error('[Home] 加载轮播图列表失败:', err);
      });
  },

  /**
   * swiper 切换时懒加载当前索引图片
   */
  onMemorySwiperChange(e) {
    const index = e.detail.current;
    this._ensureBannerImageLoaded(index);
  },

  /**
   * 确保指定索引的轮播图已加载，如未加载则调用图片接口并写入缓存
   * @param {Number} index 轮播图索引
   */
  _ensureBannerImageLoaded(index) {
    const list = this.data.memoryImages || [];
    if (!list.length || index < 0 || index >= list.length) return;

    const item = list[index];
    if (!item || item.loaded || !item.imagePath) return;

    bannerApi.getBannerImage(item.imagePath)
      .then(buffer => {
        try {
          const base64 = wx.arrayBufferToBase64(buffer);
          // 简单根据后缀猜测 mime 类型，默认 jpeg
          let mime = 'image/jpeg';
          if (item.imagePath.endsWith('.png')) {
            mime = 'image/png';
          } else if (item.imagePath.endsWith('.gif')) {
            mime = 'image/gif';
          } else if (item.imagePath.endsWith('.webp')) {
            mime = 'image/webp';
          }
          const src = `data:${mime};base64,${base64}`;

          const updated = this.data.memoryImages.slice();
          if (!updated[index]) return;
          updated[index].src = src;
          updated[index].loaded = true;
          this.setData({ memoryImages: updated });
        } catch (e) {
          console.error('[Home] 处理轮播图图片数据失败:', e);
        }
      })
      .catch(err => {
        console.error('[Home] 加载轮播图图片失败:', err);
      });
  },

  /**
   * 展开/收起 成员审批列表
   */
  onToggleApproval() {
    const next = !this.data.showApproval;
    this.setData({
      showApproval: next,
      showFamilySwitch: false
    });
    if (next && this.data.pendingMembers.length === 0) {
      this._loadPendingMembers(this.data.currentFamily.familyId, false);
    }
  },

  /**
   * 审批通过
   */
  onApprove(e) {
    const memberId = e.currentTarget.dataset.memberId;
    const familyId = this.data.currentFamily.familyId;

    wx.showLoading({ title: '处理中...' });
    familyApi.approveJoin(familyId, memberId)
      .then(() => {
        wx.hideLoading();
        wx.showToast({ title: '已通过申请', icon: 'success', duration: 1500 });
        this._loadPendingMembers(familyId, false);
        // 刷新成员数
        this._refreshFamilyData();
      })
      .catch(err => {
        wx.hideLoading();
        wx.showToast({ title: '操作失败，请重试', icon: 'none' });
        console.error('[Home] 审批通过失败:', err);
      });
  },

  /**
   * 审批拒绝
   */
  onReject(e) {
    const memberId = e.currentTarget.dataset.memberId;
    const familyId = this.data.currentFamily.familyId;

    wx.showModal({
      title: '确认拒绝',
      content: '确定要拒绝该成员的申请吗？',
      confirmColor: '#ff4b6e',
      success: res => {
        if (!res.confirm) return;
        wx.showLoading({ title: '处理中...' });
        familyApi.rejectJoin(familyId, memberId)
          .then(() => {
            wx.hideLoading();
            wx.showToast({ title: '已拒绝申请', icon: 'none', duration: 1500 });
            this._loadPendingMembers(familyId, false);
          })
          .catch(err => {
            wx.hideLoading();
            wx.showToast({ title: '操作失败，请重试', icon: 'none' });
            console.error('[Home] 审批拒绝失败:', err);
          });
      }
    });
  },

  /**
   * 跳转到创建/加入家庭引导页
   */
  onAddFamilyTap() {
    wx.navigateTo({ url: '/pages/family/guide/guide' });
  },

  // ============================
  //   首页 Tab 功能菜单
  // ============================

  navigateToLove() {
    wx.showToast({ title: '执子之手开发中', icon: 'none' });
  },

  navigateToHome() {
    wx.showToast({ title: '平安喜乐开发中', icon: 'none' });
  },

  navigateToImportant() {
    wx.navigateTo({ url: '/pages/event/list/list' });
  },

  /**
   * 复制家庭编号（保留备用，页面未展示，可按需恢复）
   */
  onCopyCode() {
    const code = this.data.currentFamily && this.data.currentFamily.familyCode;
    if (!code) return;
    wx.setClipboardData({
      data: code,
      success: () => {
        wx.showToast({ title: '编号已复制', icon: 'success', duration: 1500 });
      }
    });
  },

  // ============================
  //   提醒功能
  // ============================

  /**
   * 静默加载当前家庭的活跃提醒数量，更新红点
   * @param {Number} familyId 家庭ID
   */
  _loadRemindCount(familyId) {
    if (!familyId) return;
    remindApi.getRemindCount(familyId)
      .then(count => {
        this.setData({ remindCount: count || 0 });
        console.log('[Home] 提醒红点数量:', count);
      })
      .catch(err => {
        console.error('[Home] 加载提醒数量失败:', err);
      });
  },

  /**
   * 展开提醒浮层，同时拉取活跃提醒列表
   * 后端在此接口调用时同步将 PENDING 标记为 READ
   */
  onOpenRemindPanel() {
    const familyId = this.data.currentFamily && this.data.currentFamily.familyId;
    if (!familyId) return;

    this.setData({ showRemindPanel: true, remindLoading: true, remindList: [] });
    console.log('[Home] 展开提醒浮层, familyId=' + familyId);

    remindApi.getActiveReminds(familyId)
      .then(list => {
        const remindList = (list || []).map(item => this._buildRemindItem(item));
        this.setData({ remindList, remindLoading: false });
      })
      .catch(err => {
        this.setData({ remindLoading: false });
        console.error('[Home] 加载提醒列表失败:', err);
      });
  },

  /**
   * 关闭提醒浮层
   */
  onCloseRemindPanel() {
    this.setData({ showRemindPanel: false });
  },

  /**
   * 构建单条提醒的展示数据（计算日期文案、颜色类、emoji）
   * @param {Object} item RemindLogVO
   * @return {Object} 扩展后的展示对象
   */
  _buildRemindItem(item) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const eventDate = new Date(item.eventDate);
    eventDate.setHours(0, 0, 0, 0);
    const diffDays = Math.round((eventDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    let dateDesc = '';
    let dateClass = 'date-normal';
    if (diffDays < 0) {
      dateDesc = '已过期（' + item.eventDate + '）';
      dateClass = 'date-expired';
    } else if (diffDays === 0) {
      dateDesc = '就是今天（' + item.eventDate + '）';
      dateClass = 'date-today';
    } else if (diffDays <= 3) {
      dateDesc = '还有 ' + diffDays + ' 天（' + item.eventDate + '）';
      dateClass = 'date-soon';
    } else {
      dateDesc = '还有 ' + diffDays + ' 天（' + item.eventDate + '）';
      dateClass = 'date-normal';
    }

    var advanceDesc = item.remindAdvanceDays > 0
      ? '提前 ' + item.remindAdvanceDays + ' 天提醒'
      : '当天提醒';

    var emojiMap = {
      BIRTHDAY: '🎂',
      ANNIVERSARY: '💍',
      HOLIDAY: '🎉',
      DOCUMENT_EXPIRY: '🪪',
      HEALTH: '🩺',
      PAYMENT: '💳',
      OTHER: '📌'
    };

    return {
      remindLogId: item.remindLogId,
      eventId: item.eventId,
      title: item.title,
      category: item.category,
      status: item.status,
      eventDate: item.eventDate,
      remindAdvanceDays: item.remindAdvanceDays,
      dateDesc: dateDesc,
      dateClass: dateClass,
      advanceDesc: advanceDesc,
      categoryEmoji: emojiMap[item.category] || '📌'
    };
  },

  /**
   * 标记某条提醒为"已完成"
   */
  onRemindDone(e) {
    var remindLogId = e.currentTarget.dataset.remindLogId;
    var index = e.currentTarget.dataset.index;
    console.log('[Home] 标记完成, remindLogId=' + remindLogId);

    wx.showLoading({ title: '处理中...' });
    remindApi.doneRemind(remindLogId)
      .then(() => {
        wx.hideLoading();
        wx.showToast({ title: '已标记完成 ✓', icon: 'none', duration: 1500 });
        this._removeRemindItem(index);
      })
      .catch(err => {
        wx.hideLoading();
        console.error('[Home] 标记完成失败:', err);
      });
  },

  /**
   * 忽略/关闭一条提醒
   */
  onRemindClose(e) {
    var remindLogId = e.currentTarget.dataset.remindLogId;
    var index = e.currentTarget.dataset.index;
    console.log('[Home] 忽略提醒, remindLogId=' + remindLogId);

    wx.showLoading({ title: '处理中...' });
    remindApi.closeRemind(remindLogId)
      .then(() => {
        wx.hideLoading();
        this._removeRemindItem(index);
      })
      .catch(err => {
        wx.hideLoading();
        console.error('[Home] 忽略提醒失败:', err);
      });
  },

  /**
   * 从列表移除已操作的条目，同步更新红点数
   * 列表全部清空后延迟自动关闭浮层
   * @param {Number} index 条目索引
   */
  _removeRemindItem(index) {
    var list = this.data.remindList.slice();
    list.splice(index, 1);
    var newCount = Math.max(0, this.data.remindCount - 1);
    this.setData({ remindList: list, remindCount: newCount });

    if (list.length === 0) {
      setTimeout(() => {
        this.setData({ showRemindPanel: false });
      }, 800);
    }
  }
});
