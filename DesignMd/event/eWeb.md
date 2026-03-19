# 家庭重要事项（Event）— 小程序前端页面设计

> 目标：基于 `DesignMd/family/web_design.md` 的视觉与交互风格，为「五、家庭重要事项基础功能」补齐**创建 / 查询（列表+详情）/ 修改（前端预留）/ 删除**的前端页面与交互闭环。  
> 后端接口已实现：见 `MyFamilyBack/.../EventController.java`（`/api/event/**`）。

---

## 一、入口与导航（从家庭主页开始）

- **入口位置**：`MyFamilyWeb/pages/family/home/home.wxml` 中的「重要事项」菜单项（`bindtap="navigateToImportant"`）。
- **跳转目标**：`/pages/event/list/list`
- **返回策略**：
  - 列表页、编辑页均采用 `navigationStyle: custom`，顶部自绘返回按钮，默认 `wx.navigateBack()`。

页面路由（新增）：

```
/pages/event/list/list    重要事项列表（查询 + 搜索 + 删除入口）
/pages/event/edit/edit    新建/编辑事项（创建 + 详情展示 + 删除入口 + 修改预留）
```

---

## 二、接口对齐（与后端一致）

接口来自 `EventController.java`：

| 场景 | 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|---|
| 创建 | POST | `/api/event/create` | Body: `CreateEventRequest` | 创建成功返回 `EventDetailVO` |
| 列表 | GET | `/api/event/list` | Query: `familyId` | 返回 `List<EventBriefVO>`（含 `isOwner`） |
| 详情 | GET | `/api/event/detail` | Query: `eventId` | 返回 `EventDetailVO`（含 `isOwner`） |
| 删除 | DELETE | `/api/event/delete` | Query: `eventId` | 仅创建人可删除 |

**修改（Update）说明**：
- 当前后端未提供 `/api/event/update`。本次前端已把「编辑页 UI + 数据回填」做完，并在保存时给出提示：待后端补充 update 接口即可直接启用。
- 建议后端新增：`POST /api/event/update`（或 `PUT /api/event/update`），入参可复用 `CreateEventRequest + eventId` 或单独 `UpdateEventRequest`。

---

## 三、数据结构与字段（前端关心点）

### 1）事件核心字段

- **title**：事项名称（必填，≤100）
- **category**：`BIRTHDAY / ANNIVERSARY / HOLIDAY / DOCUMENT_EXPIRY / HEALTH / PAYMENT / OTHER`（必填）
- **dateType**：`0=阳历`，`1=农历`（必填，默认 0）
- **month/day**：月、日（必填）
- **description**：备注（选填，≤500）

### 2）提醒字段（Plan 六预埋，Plan 五阶段保存）

- **remindEnabled**：0/1
- **remindAdvanceDays**：提前天数（0=当天）
- **remindTarget**：`ALL / SPECIFIC`
- **remindUserId**：当 `SPECIFIC` 时有效（当前前端仅预留提示，指定成员选择待成员模块完善）

### 3）权限字段

- **isOwner**：后端在 list/detail 返回，前端用来控制「删除/编辑按钮」展示。

---

## 四、页面设计（风格：延续 web_design.md 的卡片化与渐变头部）

### A. 重要事项列表页 `pages/event/list/list`

#### 1）页面定位
展示当前家庭的所有重要事项，支持：
- **查询**：按家庭维度拉取列表（`GET /api/event/list?familyId=...`）
- **搜索**：本地过滤（标题/创建人）
- **分类筛选**：本地过滤（类别 Chip）
- **删除**：仅创建人可见（调用 `DELETE /api/event/delete`）
- **新增**：顶部“新增”按钮 + 右下角 FAB

#### 2）布局结构
- **顶部渐变头部**（绿色系，对应“重要事项”模块）
  - 左：返回
  - 中：标题“重要事项” + 当前家庭名
  - 右：新增
  - 下：搜索框
- **分类筛选条**（横向滚动 chip）
- **统计卡**：显示当前筛选后的数量
- **列表卡片**：
  - 左侧彩色徽标（按 category 映射 emoji + 渐变底色）
  - 中部：标题、标签（提醒/农历）、元信息（MM-DD + 创建人）
  - 右侧：如果 `isOwner` 显示「编辑/删除」两枚 mini button；否则显示箭头
- **空态**：引导新增第一个事项

#### 3）交互规则
- **点击条目**：进入编辑页（携带 `eventId`），在编辑页展示详情回填
- **删除**：二次确认；成功后刷新列表
- **下拉刷新**：重新拉取列表

#### 4）类别映射（前端表现）
- 生日 🎂、纪念日 💍、节假日 🎉、证件 🪪、健康 🩺、缴费 💳、其他 📌

---

### B. 新建/编辑页 `pages/event/edit/edit`

#### 1）页面定位
- **新建**：填写表单并创建事项（`POST /api/event/create`）
- **编辑**：
  - **详情回填**：`GET /api/event/detail?eventId=...`
  - **删除入口**：仅创建人可见
  - **保存修改**：当前后端无 update 接口，本次前端先提示“待后端补齐后启用”

#### 2）布局结构
- 顶部渐变头部（同列表页）
  - 左：返回
  - 中：标题「新建事项/编辑事项」+ 家庭名
  - 右：创建/保存按钮
- 表单卡（核心字段）
  - 事项名称 input
  - 类别 picker
  - 日期类型 segmented（阳历/农历）
  - 月/日 picker（分两列）
  - 备注 textarea
- 提醒设置卡
  - remindEnabled switch
  - 开启时展示：
    - 提前几天提醒 picker（0/1/2/3/5/7/10/15/30）
    - 提醒对象 segmented（全家/指定）+ 预留提示
- 权限卡（仅编辑态展示）
  - 创建信息（创建人/时间）
  - 删除按钮（仅创建人）

#### 3）校验与反馈
- 必填校验：title 非空、month/day 合法
- 成功：toast + 返回上一页（列表页自动刷新）
- 失败：统一走 `utils/request.js` 的错误处理（401 跳登录、403 toast、5xx toast）

---

## 五、实现清单（已落地到代码）

### 1）新增文件
- `MyFamilyWeb/utils/eventApi.js`
- `MyFamilyWeb/pages/event/list/list.{js,wxml,wxss,json}`
- `MyFamilyWeb/pages/event/edit/edit.{js,wxml,wxss,json}`

### 2）改动文件
- `MyFamilyWeb/utils/cloudRequest.js`：新增 `del()` 支持 DELETE
- `MyFamilyWeb/utils/request.js`：新增 `del()` 业务层封装
- `MyFamilyWeb/pages/family/home/home.js`：`navigateToImportant()` 跳转列表页
- `MyFamilyWeb/pages/index/index.js`：`navigateToImportant()` 跳转列表页（与家庭入口一致）
- `MyFamilyWeb/app.json`：注册新页面路由

---

## 六、后续扩展（与计划六/七衔接）

- **指定成员提醒（SPECIFIC）**：待成员列表/选择组件完善后，补齐成员选择弹层，并写入 `remindUserId`
- **修改接口**：后端补齐 `/api/event/update` 后，编辑页的保存按钮可直接改为调用 update，并保持字段兼容当前表结构
- **提醒调度与订阅消息**：后续阶段实现时，本页面只需继续承载提醒配置的编辑即可

