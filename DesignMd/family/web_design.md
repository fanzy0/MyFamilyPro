## 家庭模块前端页面设计（小程序 Web 视角）

本设计基于现有首页 `pages/index/index` 和登录页 `pages/login/login` 的视觉与交互风格，新增家庭相关页面与入口，服务于“创建/加入家庭 + 户主审批”完整闭环。

### 1. 整体导航与入口设计

- **现有首页 `pages/index/index` 调整点**：
  - 保持现有顶部“积善之家必有余庆”+轮播图区域不变。
  - 保持功能菜单三块：`navigateToLove` / `navigateToHome` / `navigateToImportant`，其中：
    - `navigateToHome`：后续可跳转到“家庭主页（family/home）”，当前仍提示“开发中”，等家庭主页实现后再切。
  - 底部 Tab 保持“首页 / 我的”结构不变。
- **家庭模块入口**（首版）：
  - 登录成功后，`pages/login/login.js` 中 `_navigateToMain`：
    - `familyList.length === 0`：跳转到 `pages/family/guide`（创建/加入引导页）。
    - `familyList.length > 0`：设置 `currentFamilyId` 后跳转家庭主页 `pages/family/home`（后续实现）。
  - 户主审批入口：
    - 在未来的家庭主页 `pages/family/home` 中，提供“成员管理/待审批”入口跳转到 `pages/family/approve`。

### 2. 创建/加入家庭引导页 `pages/family/guide`

- **页面定位**：
  新用户在完成登录但尚未加入任何家庭时的第一个落地点，引导用户完成“创建我的家庭”或“加入现有家庭”。
- **信息结构**：
  - 顶部：品牌/标题区，延续登录页风格：
    - 标题：“欢迎加入我们的家”
    - 副标题：“先创建或加入一个家庭，开始管理重要事项”
  - 中部：两个主卡片按钮：
    - 卡片 A：“创建我的家庭”：
      - 图标：🏠 或 `home.svg`
      - 文案：“以你为户主，创建一个新的家庭”
      - 操作：`bindtap="onCreateTap"` → `wx.navigateTo({ url: '/pages/family/create/create' })`
    - 卡片 B：“加入现有家庭”：
      - 图标：👨‍👩‍👧‍👦
      - 文案：“通过家庭编号加入亲人的家庭”
      - 操作：`bindtap="onJoinTap"` → `wx.navigateTo({ url: '/pages/family/join/join' })`
  - 底部：说明文案：
    - “你可以稍后在‘家庭主页’中切换或退出家庭（后续版本支持）”
- **状态与交互**：
  - 不涉及网络请求，仅负责导航。
  - 从登录 `_navigateToMain` 进入时，不展示返回按钮（通过页面配置或逻辑处理）。

### 3. 创建家庭页面 `pages/family/create`

- **页面目标**：
  让当前登录用户以简洁表单完成家庭创建，并在成功后跳转到家庭主页。
- **结构布局**：
  - 标题区：
    - 主标题：“创建我的家庭”
    - 副标题：“以你为户主，管理家庭中的重要事项与提醒”
  - 表单区：
    - 输入项 1：`家庭名称`
      - `input` 组件，`placeholder` 示例：“例如：张三家庭”
      - 默认值策略：如果 `app.globalData.currentUser.nickname` 存在，placeholder 可为“{nickname}家庭”。
    - 提示文案：
      “家庭名称仅用于展示，成员加入时可看到该名称。”
  - 底部操作区：
    - 主按钮：“创建家庭”
      - `loading` 状态绑定 `submitting`，防止重复提交。
- **交互与流程**：
  - `onCreateTap` 逻辑（与 `fPlan.md` 对齐）：
    1. 本地校验家庭名称非空、长度合理（2–20 字符）。
    2. `wx.showLoading({ title: '创建中...' })` + `submitting = true`。
    3. 调用接口 `POST /api/family/create`（经统一 `cloudRequest` 封装）。
    4. 后端返回 `FamilyDO`（含 `id/familyName/familyCode/...`）：
       - 更新 `app.globalData.currentFamilyId = family.id`。
       - 调用 `GET /api/family/my-families` 刷新 `familyList`（确保登录模块、首页和引导页看到一致数据）。
       - 跳转家庭主页：`wx.redirectTo({ url: '/pages/family/home/home' })`（家庭主页后续实现）。
    5. 失败：toast 提示“创建失败：xxx”，保持在当前页允许重试。
    6. 结束：`wx.hideLoading()`，`submitting = false`。
- **异常与提示**：
  - 400/参数错误：展示后端返回的 message（如“家庭名称不能为空”）。
  - 401/未登录：引导回登录页 `pages/login/login`。

### 4. 加入家庭页面 `pages/family/join`

- **页面目标**：
  通过输入家庭编号加入家庭，并给用户足够的预览与确认信息，避免误操作。
- **结构布局**：
  - 标题区：
    - 主标题：“加入家庭”
    - 副标题：“向户主要来的家庭编号，申请加入他/她的家庭”
  - 输入区：
    - 文本输入：`家庭编号`
      - `input`，`placeholder`：“请输入 8 位家庭编号”
  - 预览区（可选显示）：
    - 当查询成功后显示卡片：
      - 家庭名称：例如“张三家庭”
      - 成员数量：例如“已有 3 位家庭成员”
      - 简短提示：“确认这是你要加入的家庭”
  - 操作区：
    - 按钮 1：“查询家庭信息”
    - 按钮 2：“申请加入”（仅在查询成功后启用）
- **交互与流程**：
  - `onQueryFamily`：
    1. 校验 `familyCode` 非空。
    2. 调用 `GET /api/family/by-code?familyCode=xxx`。
    3. 成功：将返回的 `FamilyDO` 存入 `data.familyPreview`，展示预览卡片。
    4. 失败：toast “家庭不存在或编号错误”，清空预览。
  - `onApplyJoin`：
    1. 校验已经有 `familyPreview`，否则提示先查询。
    2. `wx.showLoading({ title: '提交申请中...' })`。
    3. 调用 `POST /api/family/join`，Body：`{ familyCode }`。
    4. 成功：toast “已提交申请，等待户主审批”，`wx.navigateBack()` 或返回引导页。
    5. 典型业务错误：
       - “您已在该家庭中”：toast 后提示可回到家庭主页。
       - “您已申请加入该家庭，请等待户主审批”：toast 并不重复提交。
    6. 完成后 `hideLoading`。

### 5. 户主审批页面 `pages/family/approve`

- **页面目标**：
  为家庭户主提供一个清晰的“待审批成员列表”，可对每条申请进行“同意/拒绝”操作。
- **入口设计**：
  - 暂定从未来的家庭主页 `pages/family/home` 中进入：一个“成员审批”菜单项。
- **结构布局**：
  - 标题区：
    - 主标题：“成员审批”
    - 副标题：“仅家庭户主可在此批准或拒绝加入申请”
  - 列表区：
    - 使用 `scroll-view` 或普通 `view` 列表渲染接口返回的 `pendingList`：
      - 显示项：
        - 成员昵称（无昵称时显示“家庭成员”）
        - 申请时间（`create_time` 格式化）
      - 行内操作按钮：
        - 左按钮：“同意”
        - 右按钮：“拒绝”
  - 空态展示：
    - 当 `pendingList.length === 0`：
      - 图标：✅
      - 文案：“当前没有待审批的成员申请”
- **交互与流程**：
  - `onLoad/onShow`：
    1. 从 `app.globalData.currentFamilyId` 读取 `familyId`（若不存在则提示并返回上一页）。
    2. 调用 `GET /api/family/pending-members?familyId=xxx`。
    3. 将返回的 `FamilyMemberDO` 数组绑定到 `pendingList`。
  - `onApproveTap(e)`：
    1. 取出 `familyMemberId`。
    2. 调用 `POST /api/family/approve`，Body：`{ familyId, familyMemberId }`。
    3. 成功后从列表中移除该项，toast “已同意加入”。
    4. 若后端返回“成员状态已变化，请刷新后重试”，弹出提示并重新拉取列表。
  - `onRejectTap(e)`：
    - 类似 `onApproveTap`，调用 `POST /api/family/reject`，成功提示“已拒绝加入”。

### 6. 与现有登录/首页的衔接小结

- 登录页 `pages/login/login`：
  - 调用 `/api/auth/login` 后，已经将 `familyList` 写入 `app.globalData.familyList`。
  - `_navigateToMain` 建议调整为：
    - `familyList.length === 0` → `wx.redirectTo({ url: '/pages/family/guide/guide' })`
    - `familyList.length > 0` → 设置 `currentFamilyId`，再跳转家庭主页（后续实现）：`wx.redirectTo({ url: '/pages/family/home/home' })`
- 首页 `pages/index/index`：
  - 保持现有功能与布局，只在家庭模块开发完成后，将 `navigateToHome` 改为跳转家庭主页，形成从首页 → 家庭主页 → 成员审批的导航闭环。

整体上，这些设计保证：
- 登录成功后一定先落到“是否有家庭”的判定，再引导用户创建/加入家庭。
- 户主可以从家庭域内部进入审批页完成成员管理，而普通成员只看到与自己家庭相关的数据。
- 视觉与交互上延续现有首页/登录的文案风格与布局节奏，避免割裂感。

