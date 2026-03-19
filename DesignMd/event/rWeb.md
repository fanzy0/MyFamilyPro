# 七、提醒调度 — 小程序前端页面设计（rWeb）

> 依赖：
> - 后端设计：`DesignMd/event/rPlan.md`
> - 事项页面设计：`DesignMd/event/eWeb.md`（已实现的列表页 / 编辑页）
> - 家庭主页：`MyFamilyWeb/pages/family/home/home.{wxml,js,wxss}`
> - 风格规范：`DesignMd/family/web_design.md`
>
> 本次前端新增两个功能区域：
> 1. **家庭主页 — 悬浮提醒入口 + 提醒列表浮层**
> 2. **事项编辑/详情页 — 历年完成情况卡片**

---

## 一、接口对齐（后端已实现）

| 场景 | 方法 | 路径 | 参数 | 响应 |
|---|---|---|---|---|
| 红点数量 | GET | `/api/remind/count` | Query: `familyId` | `Integer` |
| 活跃提醒列表（同时标记已读） | GET | `/api/remind/active` | Query: `familyId` | `List<RemindLogVO>` |
| 标记已完成 | POST | `/api/remind/done` | Body: `{remindLogId}` | `"OK"` |
| 忽略/关闭提醒 | POST | `/api/remind/close` | Body: `{remindLogId}` | `"OK"` |
| 历年提醒记录 | GET | `/api/remind/history` | Query: `eventId` | `List<RemindHistoryVO>` |

### RemindLogVO（活跃提醒）
```
remindLogId       Long        操作时回传
eventId           Long
title             String      事项名称
category          String      BIRTHDAY/ANNIVERSARY/...
status            String      PENDING / READ
eventDate         yyyy-MM-dd  当年事件日期（阳历）
remindDate        yyyy-MM-dd  应开始提醒日期
remindAdvanceDays Integer     提前天数
```

### RemindHistoryVO（历年记录）
```
remindLogId   Long
triggerYear   Integer         如 2026
eventDate     yyyy-MM-dd
status        String          DONE/CLOSED_BY_USER/CLOSED_BY_SYSTEM/PENDING/READ
actionTime    yyyy-MM-dd HH:mm:ss   终态时间，进行中为 null
```

---

## 二、新增 / 改动文件清单

```
新增：
  MyFamilyWeb/utils/remindApi.js         提醒相关 API 封装

改动：
  MyFamilyWeb/pages/family/home/home.js   新增提醒红点加载 + 浮层逻辑
  MyFamilyWeb/pages/family/home/home.wxml 新增悬浮按钮 + 提醒浮层
  MyFamilyWeb/pages/family/home/home.wxss 新增浮层相关样式

  MyFamilyWeb/pages/event/edit/edit.js    新增加载历年记录逻辑
  MyFamilyWeb/pages/event/edit/edit.wxml  新增"历年完成情况"卡片
  MyFamilyWeb/pages/event/edit/edit.wxss  新增历年卡片样式
```

---

## 三、功能区域A：悬浮提醒入口 + 浮层（家庭主页）

### 3.1 视觉布局

```
┌──────────────────────────────┐
│  家庭主页（scroll-view）       │
│                              │
│  ...家庭内容...               │
│                              │
│                    ┌──────┐  │  ← 悬浮按钮（fixed，右下角，bottom-nav 上方）
│                    │ 📋 3 │  │    绿色圆形按钮 + 右上角红色数字角标
│                    └──────┘  │
│  [底部导航栏]                 │
└──────────────────────────────┘

点击悬浮按钮后，从底部弹出提醒浮层：

┌──────────────────────────────┐
│  📋  即将到来的提醒      ✕   │  ← 浮层标题 + 关闭按钮
│──────────────────────────────│
│  🎂  妈妈生日                │  ← 条目：左侧 category emoji
│      还有 3 天（4月10日）     │  ←       事件日期倒计时
│      提前3天提醒             │  ←       提前天数标签
│           [✓ 完成]  [× 忽略] │  ←       操作按钮
│──────────────────────────────│
│  💍  结婚纪念日               │
│      今天（4月15日）          │
│      当天提醒                │
│           [✓ 完成]  [× 忽略] │
│──────────────────────────────│
│  （无更多提醒）               │
└──────────────────────────────┘
```

### 3.2 交互流程

```
onShow / _refreshFamilyData 完成
        │
        ▼
调用 GET /api/remind/count?familyId=xxx
        │
   count > 0 ─────────────► 显示悬浮按钮（remindCount = count）
        │
   count = 0 ─────────────► 隐藏悬浮按钮

用户点击悬浮按钮
        │
        ▼
showRemindPanel = true  → 底部浮层弹出
调用 GET /api/remind/active?familyId=xxx
        │
        ▼
渲染提醒列表（同时服务端将 PENDING → READ）

用户点击「✓ 完成」
        │
        ▼
POST /api/remind/done { remindLogId }
成功 → 从列表移除该条 → 刷新 remindCount（-1）
失败 → wx.showToast 提示

用户点击「× 忽略」
        │
        ▼
POST /api/remind/close { remindLogId }
成功 → 从列表移除该条 → 刷新 remindCount（-1）

列表全部处理完 → 自动关闭浮层，隐藏悬浮按钮
```

### 3.3 data 新增字段（home.js）

```javascript
// 提醒相关
remindCount: 0,           // 活跃提醒数量（红点数字）
showRemindPanel: false,   // 是否展开提醒浮层
remindList: [],           // 活跃提醒列表
remindLoading: false,     // 浮层加载状态
```

### 3.4 WXML 结构（home.wxml 新增部分）

**位置**：`</scroll-view>` 与 `<!-- 底部固定导航 -->` 之间，在 `wx:if="{{!loading && currentFamily}}"` 条件下渲染。

```xml
<!-- 提醒悬浮按钮（count > 0 时显示） -->
<view
  class="remind-fab"
  wx:if="{{remindCount > 0}}"
  bindtap="onOpenRemindPanel"
>
  <text class="remind-fab-icon">📋</text>
  <view class="remind-fab-badge" wx:if="{{remindCount > 0}}">
    <text class="remind-fab-badge-text">{{remindCount > 99 ? '99+' : remindCount}}</text>
  </view>
</view>

<!-- 提醒浮层遮罩 -->
<view
  class="remind-mask"
  wx:if="{{showRemindPanel}}"
  bindtap="onCloseRemindPanel"
></view>

<!-- 提醒浮层主体（底部弹出） -->
<view class="remind-panel {{showRemindPanel ? 'remind-panel-show' : ''}}">

  <!-- 浮层标题栏 -->
  <view class="remind-panel-header">
    <view class="remind-panel-title-wrap">
      <text class="remind-panel-icon">📋</text>
      <text class="remind-panel-title">即将到来的提醒</text>
      <text class="remind-panel-count" wx:if="{{remindList.length > 0}}">{{remindList.length}}条</text>
    </view>
    <view class="remind-panel-close" bindtap="onCloseRemindPanel">
      <text class="remind-close-icon">✕</text>
    </view>
  </view>

  <!-- 加载中 -->
  <view class="remind-loading" wx:if="{{remindLoading}}">
    <text class="remind-loading-text">加载中...</text>
  </view>

  <!-- 空态 -->
  <view class="remind-empty" wx:elif="{{remindList.length === 0}}">
    <text class="remind-empty-icon">🎉</text>
    <text class="remind-empty-text">暂无待处理提醒</text>
  </view>

  <!-- 提醒列表 -->
  <scroll-view scroll-y class="remind-list" wx:else>
    <view
      class="remind-item"
      wx:for="{{remindList}}"
      wx:key="remindLogId"
    >
      <!-- 左侧 emoji + 日期信息 -->
      <view class="remind-item-left">
        <view class="remind-item-emoji-wrap">
          <text class="remind-item-emoji">{{item.categoryEmoji}}</text>
        </view>
        <view class="remind-item-info">
          <text class="remind-item-title">{{item.title}}</text>
          <text class="remind-item-date {{item.dateClass}}">{{item.dateDesc}}</text>
          <text class="remind-item-advance" wx:if="{{item.remindAdvanceDays > 0}}">提前{{item.remindAdvanceDays}}天提醒</text>
          <text class="remind-item-advance" wx:else>当天提醒</text>
        </view>
      </view>

      <!-- 右侧操作按钮 -->
      <view class="remind-item-actions">
        <view
          class="btn-remind-done"
          bindtap="onRemindDone"
          data-remind-log-id="{{item.remindLogId}}"
          data-index="{{index}}"
        >✓ 完成</view>
        <view
          class="btn-remind-close"
          bindtap="onRemindClose"
          data-remind-log-id="{{item.remindLogId}}"
          data-index="{{index}}"
        >× 忽略</view>
      </view>
    </view>
  </scroll-view>

  <!-- 底部安全区占位 -->
  <view class="remind-panel-safe"></view>
</view>
```

### 3.5 WXSS 样式要点（home.wxss 新增）

```
颜色体系：延续绿色调（与"重要事项"模块一致）
  主色：#4caf89（绿）
  强调：#ff6b6b（红点）
  背景：rgba(255,255,255,0.98)
  遮罩：rgba(0,0,0,0.45)

悬浮按钮 .remind-fab：
  position: fixed; right: 40rpx; bottom: 140rpx;（底部导航上方 30rpx）
  width: 100rpx; height: 100rpx; border-radius: 50%;
  background: linear-gradient(135deg, #4caf89, #38a57a);
  box-shadow: 0 8rpx 24rpx rgba(76,175,137,0.45);
  display: flex; align-items: center; justify-content: center;

角标 .remind-fab-badge：
  position: absolute; top: -8rpx; right: -8rpx;
  background: #ff6b6b; border-radius: 24rpx;
  min-width: 36rpx; height: 36rpx; padding: 0 8rpx;
  font-size: 20rpx; color: #fff;

遮罩 .remind-mask：
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.45); z-index: 100;

浮层 .remind-panel：
  position: fixed; left: 0; right: 0; bottom: 0;
  max-height: 70vh; border-radius: 32rpx 32rpx 0 0;
  background: #fff; z-index: 101;
  transform: translateY(100%);
  transition: transform 0.3s ease;

浮层展开 .remind-panel-show：
  transform: translateY(0);

提醒条目 .remind-item：
  display: flex; align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f0f4f8;

日期状态色：
  .date-today { color: #ff6b6b; font-weight: bold; }    今天
  .date-soon  { color: #f7a234; }                        ≤3天
  .date-normal{ color: #4caf89; }                        >3天
  .date-expired{ color: #999; }                          已过期（容错）

操作按钮 .btn-remind-done：
  background: linear-gradient(135deg, #4caf89, #38a57a);
  color: #fff; border-radius: 24rpx; padding: 10rpx 22rpx; font-size: 24rpx;

操作按钮 .btn-remind-close：
  background: #f5f5f5; color: #999;
  border-radius: 24rpx; padding: 10rpx 22rpx; font-size: 24rpx;
```

### 3.6 JS 新增方法（home.js）

```javascript
// ============================
//   提醒功能
// ============================

/**
 * 加载提醒红点数量（_refreshFamilyData 完成后调用）
 */
_loadRemindCount(familyId) {
  remindApi.getRemindCount(familyId)
    .then(count => {
      this.setData({ remindCount: count || 0 });
    })
    .catch(err => {
      console.error('[Home] 加载提醒数量失败:', err);
    });
},

/**
 * 展开提醒浮层，同时拉取活跃列表
 */
onOpenRemindPanel() {
  this.setData({ showRemindPanel: true, remindLoading: true, remindList: [] });
  const familyId = this.data.currentFamily.familyId;
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
 * 构建提醒条目展示数据（计算日期描述文案、颜色类、emoji）
 */
_buildRemindItem(item) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const eventDate = new Date(item.eventDate);
  eventDate.setHours(0, 0, 0, 0);
  const diffDays = Math.round((eventDate - today) / (1000 * 60 * 60 * 24));

  let dateDesc = '';
  let dateClass = 'date-normal';
  if (diffDays < 0) {
    dateDesc = `已过期（${item.eventDate}）`;
    dateClass = 'date-expired';
  } else if (diffDays === 0) {
    dateDesc = `今天（${item.eventDate}）`;
    dateClass = 'date-today';
  } else if (diffDays <= 3) {
    dateDesc = `还有 ${diffDays} 天（${item.eventDate}）`;
    dateClass = 'date-soon';
  } else {
    dateDesc = `还有 ${diffDays} 天（${item.eventDate}）`;
    dateClass = 'date-normal';
  }

  const emojiMap = {
    BIRTHDAY: '🎂',
    ANNIVERSARY: '💍',
    HOLIDAY: '🎉',
    DOCUMENT_EXPIRY: '🪪',
    HEALTH: '🩺',
    PAYMENT: '💳',
    OTHER: '📌'
  };

  return {
    ...item,
    dateDesc,
    dateClass,
    categoryEmoji: emojiMap[item.category] || '📌'
  };
},

/**
 * 标记某条提醒为"已完成"
 */
onRemindDone(e) {
  const { remindLogId, index } = e.currentTarget.dataset;
  wx.showLoading({ title: '处理中...' });
  remindApi.doneRemind(remindLogId)
    .then(() => {
      wx.hideLoading();
      wx.showToast({ title: '已标记完成 ✓', icon: 'none', duration: 1500 });
      this._removeRemindItem(index);
    })
    .catch(err => {
      wx.hideLoading();
      wx.showToast({ title: '操作失败，请重试', icon: 'none' });
      console.error('[Home] 标记完成失败:', err);
    });
},

/**
 * 忽略/关闭一条提醒
 */
onRemindClose(e) {
  const { remindLogId, index } = e.currentTarget.dataset;
  wx.showLoading({ title: '处理中...' });
  remindApi.closeRemind(remindLogId)
    .then(() => {
      wx.hideLoading();
      this._removeRemindItem(index);
    })
    .catch(err => {
      wx.hideLoading();
      wx.showToast({ title: '操作失败，请重试', icon: 'none' });
      console.error('[Home] 关闭提醒失败:', err);
    });
},

/**
 * 从列表移除已操作的条目，更新红点数
 */
_removeRemindItem(index) {
  const list = this.data.remindList.slice();
  list.splice(index, 1);
  const newCount = Math.max(0, this.data.remindCount - 1);
  this.setData({
    remindList: list,
    remindCount: newCount
  });
  // 列表清空则自动关闭浮层
  if (list.length === 0) {
    setTimeout(() => {
      this.setData({ showRemindPanel: false });
    }, 600);
  }
},
```

**触发时机**：`_refreshFamilyData` 最后一步（获取到 `currentFamily` 之后）调用 `this._loadRemindCount(current.familyId)`。家庭切换（`onSwitchFamily`）也需在 `setData` 之后补调 `_loadRemindCount`。

---

## 四、功能区域B：历年完成情况卡片（事项详情/编辑页）

### 4.1 视觉布局

```
┌──────────────────────────────────────┐
│  提醒设置卡（已有）                    │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐  ← 新增：历年完成情况卡
│  🔔  历年提醒情况                     │  ← 卡片标题
│──────────────────────────────────────│
│  2026年  🔔 提醒中     还有 3 天      │
│  2025年  ✅ 已完成    2025-03-15完成  │
│  2024年  ✅ 已完成    2024-03-16完成  │
│  2023年  ⏹ 已过期    （未处理）       │
│  2022年  🚫 已忽略    2022-03-18关闭  │
└──────────────────────────────────────┘
```

仅在事项**开启了提醒（remindEnabled=1）** 且 **historyList 不为空**时展示此卡。

### 4.2 data 新增字段（edit.js）

```javascript
historyList: [],       // 历年提醒记录，RemindHistoryVO 列表
historyLoading: false  // 历年记录加载状态
```

### 4.3 加载时机（edit.js）

在 `onLoad` 中，当 `options.eventId` 存在时（即编辑态），获取事项详情成功后，若 `detail.remindEnabled === 1`，追加调用历年记录接口：

```javascript
// 伪代码：onLoad 事项详情加载完成后
if (detail.remindEnabled === 1) {
  this.setData({ historyLoading: true });
  remindApi.getRemindHistory(eventId)
    .then(list => {
      const historyList = (list || []).map(item => this._buildHistoryItem(item));
      this.setData({ historyList, historyLoading: false });
    })
    .catch(() => {
      this.setData({ historyLoading: false });
    });
}

/**
 * 构建历年记录展示数据（格式化状态 emoji、文案、日期）
 */
_buildHistoryItem(item) {
  const statusMap = {
    DONE: { icon: '✅', text: '已完成', showTime: true },
    CLOSED_BY_USER: { icon: '🚫', text: '已忽略', showTime: true },
    CLOSED_BY_SYSTEM: { icon: '⏹', text: '已过期', showTime: false },
    READ: { icon: '🔔', text: '提醒中', showTime: false },
    PENDING: { icon: '🔔', text: '提醒中', showTime: false }
  };
  const s = statusMap[item.status] || { icon: '❓', text: item.status, showTime: false };

  // 计算当前年份进行中的倒计时
  let extraDesc = '';
  if (item.status === 'PENDING' || item.status === 'READ') {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const eventDate = new Date(item.eventDate);
    eventDate.setHours(0, 0, 0, 0);
    const diff = Math.round((eventDate - today) / (1000 * 60 * 60 * 24));
    if (diff > 0) {
      extraDesc = `还有 ${diff} 天`;
    } else if (diff === 0) {
      extraDesc = '今天';
    } else {
      extraDesc = '已过期';
    }
  } else if (s.showTime && item.actionTime) {
    extraDesc = item.actionTime.substring(0, 10) + (item.status === 'DONE' ? '完成' : '关闭');
  }

  return { ...item, statusIcon: s.icon, statusText: s.text, extraDesc };
}
```

### 4.4 WXML 结构（edit.wxml 新增卡片）

**位置**：提醒设置卡之后、删除按钮之前。

```xml
<!-- 历年完成情况（仅编辑态 + 开启提醒 + 有记录时展示） -->
<view
  class="form-card history-card"
  wx:if="{{isEdit && detail.remindEnabled === 1}}"
>
  <view class="card-title">
    <text class="card-title-icon">🔔</text>
    <text class="card-title-text">历年提醒情况</text>
  </view>

  <!-- 加载中 -->
  <view class="history-loading" wx:if="{{historyLoading}}">
    <text class="history-loading-text">加载中...</text>
  </view>

  <!-- 空态：提醒已开启但尚无记录 -->
  <view class="history-empty" wx:elif="{{historyList.length === 0}}">
    <text class="history-empty-text">今年提醒日到时将自动生成记录</text>
  </view>

  <!-- 历年列表 -->
  <view class="history-list" wx:else>
    <view
      class="history-item"
      wx:for="{{historyList}}"
      wx:key="remindLogId"
    >
      <text class="history-year">{{item.triggerYear}}年</text>
      <view class="history-status-wrap">
        <text class="history-status-icon">{{item.statusIcon}}</text>
        <text class="history-status-text">{{item.statusText}}</text>
      </view>
      <text class="history-extra {{item.status === 'DONE' ? 'history-done-color' : ''}}">
        {{item.extraDesc}}
      </text>
    </view>
  </view>
</view>
```

### 4.5 WXSS 样式要点（edit.wxss 新增）

```
历年卡片 .history-card：
  延续已有 .form-card 的 background / border-radius / padding / box-shadow

卡片标题 .card-title：
  display: flex; align-items: center; gap: 12rpx;
  font-size: 28rpx; font-weight: 600; color: #333;
  margin-bottom: 24rpx;

历年条目 .history-item：
  display: flex; align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f4f8;

.history-year { width: 120rpx; font-size: 26rpx; color: #555; font-weight: 500; }
.history-status-wrap { display: flex; align-items: center; gap: 8rpx; flex: 1; }
.history-status-icon { font-size: 28rpx; }
.history-status-text { font-size: 26rpx; color: #555; }
.history-extra { font-size: 24rpx; color: #999; text-align: right; }
.history-done-color { color: #4caf89; }
```

---

## 五、新增工具文件：remindApi.js

**位置**：`MyFamilyWeb/utils/remindApi.js`

```javascript
const request = require('./request.js');

/**
 * 查询活跃提醒数量（红点）
 * GET /api/remind/count?familyId=xxx
 */
function getRemindCount(familyId) {
  return request.get('/api/remind/count', { familyId });
}

/**
 * 查询活跃提醒列表（同时后端将 PENDING → READ）
 * GET /api/remind/active?familyId=xxx
 */
function getActiveReminds(familyId) {
  return request.get('/api/remind/active', { familyId });
}

/**
 * 标记提醒为已完成
 * POST /api/remind/done { remindLogId }
 */
function doneRemind(remindLogId) {
  return request.post('/api/remind/done', { remindLogId });
}

/**
 * 关闭/忽略提醒
 * POST /api/remind/close { remindLogId }
 */
function closeRemind(remindLogId) {
  return request.post('/api/remind/close', { remindLogId });
}

/**
 * 查询某事项的历年提醒记录
 * GET /api/remind/history?eventId=xxx
 */
function getRemindHistory(eventId) {
  return request.get('/api/remind/history', { eventId });
}

module.exports = { getRemindCount, getActiveReminds, doneRemind, closeRemind, getRemindHistory };
```

---

## 六、改动点汇总（精确定位到代码位置）

### home.js
| 位置 | 改动 |
|---|---|
| 文件头部 | `const remindApi = require('../../../utils/remindApi.js');` |
| `data` | 新增 `remindCount: 0, showRemindPanel: false, remindList: [], remindLoading: false` |
| `_refreshFamilyData` 成功回调末尾 | 追加 `this._loadRemindCount(current.familyId)` |
| `onSwitchFamily` 的 `setData` 之后 | 追加 `this._loadRemindCount(familyId)` |
| 新增方法 | `_loadRemindCount / onOpenRemindPanel / onCloseRemindPanel / _buildRemindItem / onRemindDone / onRemindClose / _removeRemindItem` |

### home.wxml
| 位置 | 改动 |
|---|---|
| `</scroll-view>` 之后、底部导航之前 | 插入悬浮按钮 + 遮罩 + 浮层（共约 60 行） |

### home.wxss
| 位置 | 改动 |
|---|---|
| 文件末尾追加 | 悬浮按钮、角标、遮罩、浮层、条目、操作按钮、日期颜色类等样式 |

### edit.js
| 位置 | 改动 |
|---|---|
| 文件头部 | `const remindApi = require('../../../utils/remindApi.js');` |
| `data` | 新增 `historyList: [], historyLoading: false` |
| `onLoad` 详情加载成功后 | 若 `isEdit && detail.remindEnabled === 1`，调用 `_loadHistory(eventId)` |
| 新增方法 | `_loadHistory(eventId) / _buildHistoryItem(item)` |

### edit.wxml
| 位置 | 改动 |
|---|---|
| 提醒设置卡之后、删除卡之前 | 插入历年完成情况卡片（约 35 行） |

### edit.wxss
| 位置 | 改动 |
|---|---|
| 文件末尾追加 | `.history-card / .history-item / .history-year / .history-status-* / .history-extra` 等样式 |

---

## 七、边界情况与交互细节

| 边界情况 | 处理策略 |
|---|---|
| 网络加载提醒数量失败 | 不展示悬浮按钮（`remindCount = 0`），不影响主页其他功能 |
| 浮层加载列表失败 | 展示"加载失败，请关闭重试"提示文案 |
| 点击完成/忽略后接口失败 | wx.showToast 提示"操作失败，请重试"，列表条目不删除 |
| 历年记录接口失败 | 静默降级，历年卡片不展示（不影响事项详情回填） |
| 浮层出现时底部导航被遮住 | 浮层 z-index 101 > 底部导航 z-index，符合层级预期 |
| 提醒记录已是终态再次操作 | 后端返回 409，request.js 统一 toast 提示 |
| 同时有 100+ 条活跃提醒 | scroll-view 可滚动，最大高度 70vh，无需分页（家庭小程序场景下极少发生） |

---

## 八、后续扩展（与 rPlan §5 对齐）

| 扩展点 | 说明 |
|---|---|
| 悬浮按钮点击后跳转到独立提醒页 | 若提醒条目较多，后续可新建 `pages/remind/list/list` 独立展示，悬浮按钮点击改为跳转 |
| 微信订阅消息推送 | 定时任务生成提醒时同步触发订阅消息，前端需在首次进入时 `wx.requestSubscribeMessage` |
| 历年完成情况独立页 | 事项详情下方"查看全部"→ 跳转 `pages/remind/history/history?eventId=xxx` |
| 提醒条目点击跳转事项详情 | 浮层条目点击 → `wx.navigateTo({ url: '/pages/event/edit/edit?eventId=xxx' })` |

---

*前端设计方案结束。*
