# 三、家庭（组）管理：创建与加入（实现步骤设计）

> 本文档用于指导“家庭（组）创建与加入”模块的后续开发实现，涵盖：表结构、后端接口、前端页面、审批机制与权限隔离。
>
> 约束与开发规范遵循：[MyFamilyPro/.cursor/rules/develop.mdc](MyFamilyPro/.cursor/rules/develop.mdc)
>
> 登录与鉴权前置依赖遵循：[MyFamilyPro/DesignMd/login/userAndLogin.md](MyFamilyPro/DesignMd/login/userAndLogin.md)、[MyFamilyPro/DesignMd/login/loginPlan.md](MyFamilyPro/DesignMd/login/loginPlan.md)
## 1. 设计目标与核心规则

- **目标**：支持用户创建家庭、其他用户申请加入家庭、户主审批通过/拒绝；成员仅能访问自己家庭的数据。
- **首版关键规则**：
  - **加入必须审批**：非户主加入家庭，默认进入 `PENDING`（待审批）。
  - **一个用户可加入多个家庭**（与登录设计一致：`familyList` 允许为 0..N）。
  - **当前激活家庭**：前端维护 `currentFamilyId`（首版可不落库），所有家庭相关业务接口需明确家庭上下文。
  - **数据隔离**：后端必须基于 `UserContext.userId` + `family_id` 校验成员资格（`APPROVED`）后才能访问家庭数据。

## 2. 数据库表结构设计（SQL 先行）

> 位置：`MyFamilyBack/paxl/sql/init.sql`（后续开发落表时按规范追加）

### 2.1 家庭表：`mf_family`

- **用途**：存储家庭基础信息、户主、家庭编号（用于加入）。
- **字段建议**：
  - `id` BIGINT：主键。
  - `family_name` VARCHAR(64)：家庭名称。
  - `family_code` VARCHAR(16)：家庭编号（短码），用于加入输入/搜索。
  - `owner_user_id` BIGINT：户主用户 ID（`mf_user.id`）。
  - `member_count` INT：成员数量（冗余字段，便于展示；由审批通过时维护）。
  - `status` TINYINT：状态（0=正常，1=禁用/冻结，2=已解散；首版至少用 0/1）。
  - `create_time` DATETIME：创建时间。
  - `update_time` DATETIME：更新时间。
- **索引与约束**：
  - `PRIMARY KEY (id)`
  - `UNIQUE KEY uk_family_code (family_code)`
  - `KEY idx_owner_user_id (owner_user_id)`

### 2.2 家庭成员关系表：`mf_family_member`

- **用途**：承载“成员—家庭”关系，同时承载“加入申请（PENDING）”状态（首版不单独建申请表）。
- **字段建议**：
  - `id` BIGINT：主键。
  - `family_id` BIGINT：家庭 ID（`mf_family.id`）。
  - `user_id` BIGINT：用户 ID（`mf_user.id`）。
  - `role` VARCHAR(16)：角色（`OWNER`/`MEMBER`；预留 `ADMIN`）。
  - `join_status` VARCHAR(16)：加入状态：
    - `PENDING`：待审批
    - `APPROVED`：已通过
    - `REJECTED`：已拒绝
    - `QUIT`：已退出（预留）
  - `join_time` DATETIME：审批通过时间（`APPROVED` 时写入）。
  - `create_time` DATETIME
  - `update_time` DATETIME
- **索引与约束**：
  - `PRIMARY KEY (id)`
  - `UNIQUE KEY uk_family_user (family_id, user_id)`：同一家庭同一用户只能有一条记录（避免多次申请重复插入）。
  - `KEY idx_user_id (user_id)`：查询用户加入的家庭列表。
  - `KEY idx_family_id_status (family_id, join_status)`：查询待审批列表。

### 2.3 枚举与状态约定（后端与前端统一）

- **成员角色 `role`**：
  - `OWNER`：户主（家庭创建者）
  - `MEMBER`：普通成员
- **成员状态 `join_status`**：
  - `PENDING`：申请中（只有户主可在审批列表看到）
  - `APPROVED`：已加入（可访问家庭数据）
  - `REJECTED`：被拒（可再次申请，策略见下文）
  - `QUIT`：退出（后续功能）
- **首版再申请策略**（建议）：
  - 若存在 `PENDING`：重复申请直接返回“已申请待审批”。
  - 若存在 `REJECTED/QUIT`：允许重新发起申请（将状态更新为 `PENDING`，并刷新 `update_time`）。

### 2.4 后端代码落地清单（按开发规范流程）

> 后续开发请严格按 `develop.mdc`：SQL → Entity → Mapper → XML → Service → Controller → 文档

- **Entity（DO）**：
  - `MyFamilyBack/paxl/src/main/java/com/my/family/paxl/domain/entity/FamilyDO.java`
  - `MyFamilyBack/paxl/src/main/java/com/my/family/paxl/domain/entity/FamilyMemberDO.java`
- **Mapper**：
  - `MyFamilyBack/paxl/src/main/java/com/my/family/paxl/mapper/FamilyMapper.java`
  - `MyFamilyBack/paxl/src/main/java/com/my/family/paxl/mapper/FamilyMemberMapper.java`
- **Mapper XML（手写 SQL）**：
  - `MyFamilyBack/paxl/src/main/resources/mapper/FamilyMapper.xml`
  - `MyFamilyBack/paxl/src/main/resources/mapper/FamilyMemberMapper.xml`

## 3. 后端接口与业务实现步骤（Spring Boot + MyBatis XML）

> 说明：所有接口均依赖登录模块拦截器注入 `UserContext`，从中获取 `currentUserId`；禁止前端传 `userId/openid`。
>
> 接口路径遵循：`/api/{模块}/{操作}`（见 `develop.mdc`）。

### 3.1 统一响应与错误处理约定（首版建议）

- **成功**：HTTP 200 + `ResponseEntity.ok(data)`
- **参数错误**：HTTP 400（例如家庭名为空、家庭编号不存在/格式不对）
- **鉴权失败**：HTTP 401（`X-WX-OPENID` 缺失等，已由登录模块统一处理）
- **权限不足**：HTTP 403（不是该家庭成员/不是户主）
- **业务冲突**：HTTP 409（例如已在家庭中、重复提交申请）

### 3.2 创建家庭（Create）

- **接口**：`POST /api/family/create`
- **请求 Body**：
  - `familyName`：String，必填，长度建议 2~64
- **核心步骤**：
  1. Controller 参数校验（空、长度、敏感字符过滤策略按需）。
  2. Service 取 `currentUserId`。
  3. 生成 `family_code`：短码 + 重试（必须保证唯一性）；失败需记录日志并重试/抛错。
  4. 插入 `mf_family`：`owner_user_id=currentUserId`，`member_count=1`，`status=0`。
  5. 插入 `mf_family_member`：`family_id`=新家庭，`user_id=currentUserId`，`role=OWNER`，`join_status=APPROVED`，`join_time=now()`。
  6. 返回家庭信息（含 `familyId/familyName/familyCode/role`）。
- **事务要求**：步骤 4~5 必须 `@Transactional(rollbackFor = Exception.class)`。

### 3.3 根据编号查询家庭（Join 前置，可选但强烈建议）

- **接口**：`GET /api/family/by-code?familyCode=xxxx`
- **用途**：前端加入前展示“将要加入的家庭信息”，减少输错与误加入。
- **返回**：`familyId/familyName/memberCount`（不返回敏感信息）。

### 3.4 申请加入家庭（Join）

- **接口**：`POST /api/family/join`
- **请求 Body**：
  - `familyCode`：String，必填
- **核心步骤**：
  1. 根据 `familyCode` 查询 `mf_family`，校验存在且 `status=0`。
  2. 查询 `mf_family_member` 是否已有（`family_id + user_id` 唯一）：
     - 已 `APPROVED`：返回 409 “已加入家庭”。
     - 已 `PENDING`：返回 409 “已申请，等待审批”。
     - 已 `REJECTED/QUIT`：允许再次申请（更新为 `PENDING`）。
  3. 写入申请：
     - 不存在记录：插入 `mf_family_member`，`role=MEMBER`，`join_status=PENDING`。
     - 存在 `REJECTED/QUIT`：更新该记录 `join_status=PENDING`。
  4. 返回“申请已提交”。
- **事务要求**：更新/插入成员记录建议事务（防并发）。
- **并发控制建议**：以 `uk_family_user` 为准，插入冲突时捕获后转为查询并返回“已申请/已加入”。
### 3.5 户主查看待审批列表（Pending list）

- **接口**：`GET /api/family/pending-members?familyId=xxx`
- **权限**：必须为该家庭 `OWNER`（在 `mf_family_member` 中 `role=OWNER && join_status=APPROVED`）。
- **核心步骤**：
  1. 校验当前用户是 `familyId` 的户主。
  2. 查询 `mf_family_member` where `family_id=familyId and join_status=PENDING`。
  3. 联表 `mf_user` 补充申请人展示信息（nickname/avatarUrl，允许为空，前端兜底）。
     - 由于规范要求 SQL 手写在 XML，可在 XML 中写 join。
  4. 返回列表：`familyMemberId/userId/nickname/avatarUrl/applyTime(create_time)`。

### 3.6 户主审批通过/拒绝（Approve / Reject）

- **接口**：
  - `POST /api/family/approve`
  - `POST /api/family/reject`
- **请求 Body（建议统一结构）**：
  - `familyId`：Long，必填
  - `familyMemberId`：Long，必填
  - （可选）`reason`：String，拒绝原因（首版可不存库，仅用于提示）
- **权限**：必须为该家庭 `OWNER`。
- **通过核心步骤**：
  1. 校验户主身份。
  2. 查询 `familyMemberId` 记录是否属于该 `familyId` 且当前 `join_status=PENDING`。
     - 若不是 PENDING：返回 409 “状态已变化”。
  3. 更新该成员记录：`join_status=APPROVED`，`join_time=now()`。
  4. 更新 `mf_family.member_count = member_count + 1`（只在从 PENDING→APPROVED 时执行）。
  5. 返回成功。
- **拒绝核心步骤**：
  - 类似校验后将 `join_status=REJECTED`（不更新 member_count）。
- **事务要求**：步骤 3~4 必须事务。
### 3.7 我的家庭列表（My families）

- **接口**：`GET /api/family/my-families`
- **用途**：前端刷新 `familyList`，也用于登录接口补齐家庭列表（与登录模块衔接）。
- **核心步骤**：
  1. 以 `currentUserId` 查询 `mf_family_member` where `user_id=currentUserId and join_status=APPROVED`。
  2. join `mf_family` 获取家庭信息（`family_name/family_code/member_count/status/owner_user_id`）。
  3. 返回列表项建议字段：
     - `familyId`
     - `familyName`
     - `familyCode`
     - `role`（OWNER/MEMBER）
     - `memberCount`
     - `isOwner`（可由 role 推导，前端也可直接用 role）
### 3.8 家庭数据访问的统一校验方法（强烈建议抽象）

- **Service 内部建议提供**：
  - `assertApprovedMember(familyId, userId)`：非成员/未通过则抛业务异常（映射为 403）。
  - `assertOwner(familyId, userId)`：非户主则 403。
- **后续模块复用**：家庭主页、重要事项、提醒配置等所有接口先做成员校验，再做业务逻辑。

## 4. 小程序前端页面与交互实现步骤（微信小程序原生）

> 页面目录建议：`MyFamilyWeb/pages/family/*`。每个页面包含完整的 `js/wxml/wxss/json`，并遵循 `develop.mdc` 的生命周期与错误处理规范。

### 4.1 页面清单与路由建议

- **家庭引导页**：`pages/family/guide`
  - 登录成功后，若 `familyList.length === 0` 进入该页
  - 提供「创建家庭」「加入家庭」两个入口
- **创建家庭页**：`pages/family/create`
  - 表单提交创建家庭
- **加入家庭页**：`pages/family/join`
  - 输入家庭编号（familyCode）→ 查询家庭信息（可选）→ 提交加入申请
- **户主审批页**：`pages/family/approve`
  - 户主查看 `PENDING` 成员列表并审批
- **家庭主页（后续章节实现）**：`pages/family/home`
  - 首版可先预留空页面或简单展示家庭信息与成员列表入口

### 4.2 引导页（guide）交互流程

- **页面目标**：解决“没有家庭”的新用户落地问题。
- **界面元素**：
  - 说明文案（为什么要先创建/加入家庭）
  - 主按钮：创建家庭
  - 次按钮：加入家庭
- **事件**：
  - `onCreateTap` → `wx.navigateTo({ url: '/pages/family/create/create' })`
  - `onJoinTap` → `wx.navigateTo({ url: '/pages/family/join/join' })`

### 4.3 创建家庭页（create）实现步骤

- **data**：
  - `familyName`（输入值）
  - `submitting`（防重复提交）
- **默认值策略**：
  - 若 `app.globalData.currentUser.nickname` 有值，可默认占位：`{nickname}家庭`（不强制写入 input）
  - 无昵称则占位“我的家庭”
- **提交流程**：
  1. 本地校验 `familyName` 非空、长度。
  2. `wx.showLoading`。
  3. 调用 `POST /api/family/create`。
  4. 成功：
     - 可直接调用 `GET /api/family/my-families` 刷新 `familyList`（推荐，避免依赖 create 返回字段是否齐全）
     - 设置 `app.globalData.currentFamilyId = newFamilyId`（取 create 返回或从 my-families 返回第一项匹配）
     - `wx.redirectTo({ url: '/pages/family/home/home' })`
  5. 失败：toast 提示（“创建失败，请稍后重试/家庭名不合法”等）。
  6. `wx.hideLoading`，释放 `submitting`。
### 4.4 加入家庭页（join）实现步骤

- **data**：
  - `familyCode`（输入值）
  - `familyPreview`（查询到的家庭简介，可选）
  - `submitting`
- **推荐交互**：两步提交（减少误操作）
  1. 输入 `familyCode` → 点击“查询” → 调用 `GET /api/family/by-code`，展示 `familyName/memberCount`。
  2. 点击“申请加入” → 调用 `POST /api/family/join`。
- **提交结果**：
  - 成功：toast “已提交申请，等待户主审批”，返回引导页或返回上一页。
  - 409（已申请/已加入）：toast 提示并引导去“我的家庭/家庭主页”。
### 4.5 户主审批页（approve）实现步骤

- **入口策略**：
  - 在家庭主页提供“成员审批”入口，仅 `role=OWNER` 显示
- **data**：
  - `familyId`（当前激活家庭）
  - `pendingList`（待审批列表）
  - `loading`
- **加载流程**：
  1. 读取 `app.globalData.currentFamilyId` 作为 `familyId`。
  2. `GET /api/family/pending-members?familyId=...`。
  3. 渲染列表（昵称为空则显示“家庭成员”，头像为空则默认头像）。
- **审批操作**：
  - 同意：`POST /api/family/approve`，成功后从列表移除或刷新。
  - 拒绝：`POST /api/family/reject`，成功后移除或刷新。
  - 操作期间禁用按钮防重复提交。
### 4.6 全局状态与刷新策略（与登录模块一致）

- **app.globalData 建议字段**（登录设计中已预留）：
  - `currentUser`
  - `familyList`
  - `currentFamilyId`
- **刷新时机**：
  - `pages/family/home` 的 `onShow`：调用 `GET /api/family/my-families` 刷新 `familyList`。
  - 加入申请提交后：不应把家庭直接加入 `familyList`（因为未审批），仅提示等待审批。
  - 审批通过后：被审批成员在其下次进入应用或进入家庭主页时，通过刷新 `my-families` 得到最新家庭列表。

## 5. 与登录鉴权与权限隔离的衔接（必须遵守）

### 5.1 与云托管 X-WX-OPENID 鉴权的衔接点

- **鉴权来源**：云托管注入 `X-WX-OPENID`，后端拦截器加载 `mf_user` 并注入 `UserContext`（详见 `userAndLogin.md`）。
- **接口设计约束**：
  - 家庭模块所有接口默认都处于鉴权保护之下（白名单除外）。
  - Controller 禁止接收/信任 `userId/openid`（避免越权），用户身份只能来自 `UserContext`。

### 5.2 familyList 与 currentFamilyId 的一致性策略

- **familyList**：推荐统一来自后端 `GET /api/family/my-families`（或由登录接口联查补齐）。
- **currentFamilyId**（首版建议）：
  - 前端本地维护（`app.globalData.currentFamilyId`）。
  - 登录后若 `familyList` 非空：默认选第一个家庭作为当前家庭（后续再加“家庭切换”）。
  - 创建家庭成功：将新家庭设为当前家庭。
  - 申请加入成功（PENDING）：不改变 `familyList/currentFamilyId`。

### 5.3 后端权限隔离的实现标准（后续所有模块复用）

- **成员访问控制**：所有“家庭域”的业务接口必须先校验：
  - 当前用户在 `mf_family_member` 中存在 `family_id=xxx AND user_id=currentUserId AND join_status=APPROVED`。
  - 不满足则返回 403。
- **户主权限**：审批接口必须校验：
  - `role=OWNER AND join_status=APPROVED`。
- **SQL 级过滤**：涉及家庭数据查询的 SQL 必须包含 `family_id = #{familyId}` 过滤条件（不可只在内存中过滤）。

### 5.4 登录接口的衔接（建议改造点，供后续开发参考）

> 登录设计中提到 `familyList` 首版可能先返回空数组；家庭模块完成后建议同步完善：
>
> - `POST /api/auth/login`、`GET /api/user/me` 统一调用 `FamilyService.queryMyFamilies(currentUserId)` 填充 `familyList`。
## 6. 开发执行顺序（按规范可直接照做）

1. **补齐 SQL**：在 `MyFamilyBack/paxl/sql/init.sql` 增加 `mf_family`、`mf_family_member` 建表与索引。
2. **后端落地**：按 `develop.mdc` 创建 Entity/Mapper/XML/Service/Controller，先实现 `create/join/pending/approve/reject/my-families`。
3. **后端自测**：用 Postman 带 `X-WX-OPENID` Header 覆盖核心场景：
   - 首次创建家庭
   - 申请加入（重复申请、已加入）
   - 户主审批通过/拒绝（重复审批）
   - 非成员访问 pending/approve（应 403）
4. **前端页面开发**：先 `guide/create/join`，再 `approve`（依赖户主身份与 currentFamilyId）。
5. **联调**：登录成功跳转逻辑按 `familyList` 是否为空分流；家庭主页 `onShow` 刷新 `my-families`。

## 7. 接口文档与对外契约（开发时同步维护）

> 位置建议：`MyFamilyBack/paxl/api/`（遵循 `develop.mdc` 的接口文档模板）

### 7.1 接口清单（家庭模块）

- `POST /api/family/create`：创建家庭
- `GET /api/family/by-code`：按家庭编号查询家庭（加入前预览）
- `POST /api/family/join`：申请加入家庭
- `GET /api/family/pending-members`：户主查询待审批列表
- `POST /api/family/approve`：户主审批通过
- `POST /api/family/reject`：户主审批拒绝
- `GET /api/family/my-families`：查询当前用户已加入的家庭列表

### 7.2 前后端字段对齐建议（避免联调返工）

- **familyList 列表项（建议）**：
  - `familyId`（Long）
  - `familyName`（String）
  - `familyCode`（String）
  - `role`（String：OWNER/MEMBER）
  - `memberCount`（Number）
- **pendingMembers 列表项（建议）**：
  - `familyMemberId`（Long）
  - `userId`（Long）
  - `nickname`（String，可空）
  - `avatarUrl`（String，可空）
  - `applyTime`（String/DateTime）

