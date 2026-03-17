二、用户体系与微信一键登录详细设计

本章节目标：
1. 完成微信小程序一键登录的完整链路设计（前端、云托管平台、后端、数据库）。
2. 定义用户体系的数据结构（用户主表）。
3. 明确基于云托管 X-WX-OPENID 的鉴权方案与接口协议。
4. 为后续"家庭（组）管理""家庭重要事项"等模块提供统一的用户上下文。

【前端请求方案：微信云托管（方案A）】
前端使用 wx.cloud.callContainer（已有 cloudRequest.js 封装），微信云托管平台自动向后端注入
X-WX-OPENID Header，后端直接从 Header 读取 openid，无需 code 换取流程。

--------------------------------
一、微信小程序登录整体流程
--------------------------------

1. 登录触发入口
   - 用户首次进入小程序时，展示"欢迎页 / 简要功能介绍 + 登录按钮"。
   - 交互策略：
     - 在 app.onLaunch 中调用 GET /api/user/me（X-WX-OPENID 由云托管自动携带）。
     - 若接口返回成功，则自动进入主流程（跳过登录页）。
     - 若接口失败（用户不存在或未登录），则展示登录页，引导用户点击登录按钮。

2. 登录流程时序（基于云托管 X-WX-OPENID）
   1）用户点击"微信一键登录"按钮，前端直接调用 POST /api/auth/login（通过 cloudRequest.post）。
      注：无需提前调用 wx.login() 获取 code，云托管平台已代理身份识别。
   2）wx.cloud.callContainer 发起请求时，微信云托管平台自动在请求中注入：
      - X-WX-OPENID：当前小程序下的用户唯一标识（openid）。
      - X-WX-UNIONID：若小程序已绑定开放平台账号则注入，否则为空（首版忽略）。
   3）后端从 Header 中读取 X-WX-OPENID，根据 openid 查询用户表：
      - 若存在用户：更新最近登录时间。
      - 若不存在用户：创建新用户，nickname 和 avatar_url 初始为空。
   4）后端查询该用户已加入的家庭列表（familyList），与家庭模块打通后填充，首版返回空数组。
   5）后端返回用户基础信息和 familyList，前端保存到 app.globalData，根据 familyList 跳转不同页面：
      - familyList 为空 → 跳转"创建或加入家庭"引导页。
      - familyList 非空 → 跳转家庭主页（多家庭时进入家庭选择页）。

3. 关于安全性说明
   - X-WX-OPENID 由微信云托管平台注入，客户端无法伪造，后端可直接信任。
   - 后端所有业务接口通过拦截器读取 X-WX-OPENID，加载 UserContext，实现统一鉴权。
   - 日志中不打印完整 openid（脱敏处理）。

--------------------------------
二、用户数据模型设计
--------------------------------

1. 用户主表：mf_user

   首版核心字段（仅实现以下字段）：

   - id：主键（bigint，雪花 ID）。
   - openid：微信小程序 openid，唯一索引，用于标识用户。
   - nickname：昵称（varchar，可为空；由用户在"个人资料"页主动设置，见下方说明）。
   - avatar_url：头像地址（varchar，可为空；由用户主动设置）。
   - mobile：手机号（varchar，可为空；预留，首版不实现绑定逻辑）。
   - status：用户状态（tinyint；0=正常，1=禁用）。
   - last_login_time：最近一次登录时间（datetime）。
   - create_time：创建时间（datetime）。
   - update_time：更新时间（datetime）。

   预留字段（建表时保留字段，首版不处理业务逻辑）：
   - unionid：varchar，可为空。微信开放平台 unionid，供未来多应用打通使用（v2 可选）。

   不建字段（首版明确不需要，后续按需扩展）：
   - has_family（由家庭成员关系表派生，不在用户表冗余存储）
   - country / province / city / gender / remark

   索引与约束：
   - UNIQUE(openid)：确保同一 openid 仅对应一条用户记录。
   - INDEX(mobile)：预留，若后续支持手机号登录/查找。

2. 关于用户昵称与头像的获取方式（重要）
   - 微信自 2022 年 10 月起，已停止通过 wx.getUserInfo 返回真实昵称和头像。
   - 登录时 nickname 和 avatar_url 初始为空，系统在展示时使用默认值（如"家庭成员"、默认头像图片）。
   - 引导用户在"个人资料"页主动设置：
     - 昵称：使用 <input type="nickname"> 组件，用户聚焦后可选择微信昵称填入。
     - 头像：使用 <button open-type="chooseAvatar"> 组件，用户点击后可选择微信头像上传。
   - 用户设置后，前端调用 POST /api/user/updateProfile 保存到数据库。
   - 个人资料设置页为非强制引导，用户可跳过，系统始终使用默认值兜底展示。

3. 用户登录令牌表（user_token）
   - 首版不建此表。采用云托管 X-WX-OPENID 方案，平台保证身份有效性，无需服务端维护 token 状态。
   - v2 可选：若后续需要主动下线、多终端管理、精细化 session 控制，再引入此表。

4. 与其他模块的关系
   - mf_user 与"家庭成员关系表"建立关联（后续家庭模块详细设计）。
   - 一个用户可以加入 0~N 个家庭，通过"当前激活家庭"（currentFamilyId）控制实际操作的家庭。
   - 家庭重要事项中的 create_user_id 等字段均关联 mf_user.id。

--------------------------------
三、鉴权方案（云托管 X-WX-OPENID）
--------------------------------

1. 鉴权方案说明
   - 基于微信云托管的 X-WX-OPENID Header 实现无状态鉴权。
   - 无需 JWT、无需 token 存储与管理，由云托管平台保证 openid 的真实性。

2. 后端鉴权拦截器设计
   - 拦截所有业务接口（白名单除外）。
   - 从 Header 读取 X-WX-OPENID。
   - 根据 openid 查询 mf_user 表，获取用户信息注入 UserContext（ThreadLocal）。
   - 若 X-WX-OPENID 为空或用户不存在 / 被禁用，返回 HTTP 401 + 统一错误响应。
   - 白名单：/api/auth/login 本身（但后端也会读 X-WX-OPENID 完成登录逻辑）。

3. 用户上下文（UserContext）
   - 业务层通过 UserContext.getCurrentUser() 获取当前登录用户。
   - 包含字段：userId、openid、status。

4. 异常处理
   - X-WX-OPENID 为空：返回 401，提示"身份验证失败"，记录告警日志。
   - 用户被禁用：返回 403，提示"账号已被禁用"。
   - 统一错误码格式：{ "code": "AUTH_FAILED", "message": "..." }

--------------------------------
四、后端接口设计
--------------------------------

1. 登录接口：POST /api/auth/login
   - 请求参数（Body，JSON，可选）：
     - 无必填参数；
     - deviceInfo：设备信息（首版不处理，接口预留）。
   - 关键 Header（由云托管平台自动注入，非前端传入）：
     - X-WX-OPENID：用户 openid。
   - 响应数据（JSON）：
     ```json
     {
       "user": {
         "id": 1001,
         "nickname": "",
         "avatarUrl": "",
         "status": 0
       },
       "familyList": [
         { "familyId": 1, "familyName": "张氏家庭", "role": "OWNER" }
       ]
     }
     ```
     注：首版 familyList 始终为空数组 []，与家庭模块打通后填充。
   - 主要业务逻辑：
     1）从 Header 读取 X-WX-OPENID。
     2）根据 openid 查询 mf_user，不存在则创建新用户。
     3）更新 last_login_time，记录登录日志。
     4）查询 familyList，拼装响应。

2. 获取当前用户信息接口：GET /api/user/me
   - 说明：供前端启动时校验用户是否存在、获取最新用户信息。
   - 鉴权：需要 X-WX-OPENID（云托管自动携带，无需前端额外处理）。
   - 响应数据：与登录接口中 user 字段结构一致，同时返回 familyList。

3. 更新用户基础信息接口：POST /api/user/updateProfile
   - 功能：支持用户修改昵称、头像（用户在个人资料页主动设置后调用）。
   - 权限：需鉴权；仅允许修改自己的信息。
   - 请求参数（Body，JSON）：
     - nickname：昵称（可选）。
     - avatarUrl：头像地址（可选）。
   - 首版优先级：P1，需在本阶段实现（因为 nickname/avatar_url 初始为空，用户需要有设置入口）。

4. （预留）绑定手机号接口：POST /api/user/bindMobile
   - 首版不实现，接口设计阶段仅预留路径。
   - v2 可选：使用 wx.getPhoneNumber 获取加密数据，后端解密写入 mobile 字段。

--------------------------------
五、小程序前端交互与状态管理
--------------------------------

1. 全局状态（app.globalData）
   - currentUser：当前用户基础信息（id、nickname、avatarUrl、status）。
   - familyList：用户已加入的家庭列表（首版始终为空数组）。
   - currentFamilyId：当前激活的家庭 ID，用于多家庭切换（首版为 null）。
   - isLoggedIn：登录态标志（计算属性，currentUser 非空即为 true）。

2. 请求封装增强
   - 在已有 cloudRequest.js 基础上，补充对以下情况的统一处理：
     - HTTP 401：清理 app.globalData，跳转登录页。
     - 业务错误码（如 USER_DISABLED）：展示相应 toast 提示。
     - 网络错误 / 服务器 5xx：统一展示"网络异常，请稍后重试"。
   - 不修改 cloudRequest.js 底层，新增 request.js 作为业务层封装（在其基础上增加错误处理逻辑）。

3. 登录流程前端逻辑
   - 点击"微信一键登录"按钮：
     1）调用后端 POST /api/auth/login（无需参数）。
     2）成功：将 user 和 familyList 保存到 app.globalData。
     3）根据 familyList 执行跳转：
        - familyList.length === 0 → wx.redirectTo 到"创建或加入家庭"引导页（pages/family/guide）。
        - familyList.length > 0 → wx.redirectTo 到家庭主页（pages/family/home），
          同时设置 currentFamilyId = familyList[0].familyId（首版默认取第一个）。
     4）失败（网络或服务端错误）：toast 提示"登录失败，请稍后重试"，允许用户重试。

4. 启动时自动登录校验（app.onLaunch）
   - 调用 GET /api/user/me。
   - 成功：更新 app.globalData.currentUser 和 familyList，进入主流程（同步跳转分支逻辑）。
   - 失败（用户不存在 / 401）：清理 app.globalData，跳转登录页（pages/login/login）。

5. 用户昵称与头像设置页（pages/user/profile）
   - 页面展示当前 nickname 和 avatarUrl（未设置时显示默认值）。
   - 头像选择使用：
     ```xml
     <button open-type="chooseAvatar" bindchooseavatar="onChooseAvatar">
       <image src="{{avatarUrl || '/pages/static/default-avatar.png'}}"/>
     </button>
     ```
   - 昵称输入使用：
     ```xml
     <input type="nickname" placeholder="请输入昵称" bindinput="onNicknameInput"/>
     ```
   - 提交后调用 POST /api/user/updateProfile，成功后更新 app.globalData.currentUser。
   - 此页面首版为选填引导，在家庭主页/个人中心提供入口，非强制流程。

--------------------------------
六、日志与监控（用户与登录相关）
--------------------------------

1. 后端日志
   - 记录以下关键行为：
     - 新用户首次登录（创建用户成功）。
     - 已有用户登录（更新 last_login_time）。
     - X-WX-OPENID 为空 / 用户被禁用的鉴权失败情况。
   - 日志脱敏：openid 仅记录前 6 位 + ***，不记录完整值。

2. 基础统计（v2 可选）
   - 日活跃用户数、登录失败率，为产品迭代提供参考。

--------------------------------
七、本章节输出与对后续模块的支撑
--------------------------------

1. 通过本章节设计，可以完成：
   - mf_user 用户表的创建与索引设置。
   - 后端登录接口、用户信息接口、更新资料接口及鉴权拦截器的实现。
   - 小程序端请求封装增强、登录页、启动自动校验、用户资料设置页。

2. 对后续模块的支撑：
   - 家庭（组）管理：依赖当前登录用户 id（从 UserContext 获取）完成创建/加入/审批等操作。
   - 家庭重要事项与提醒：使用 userId 作为事项创建人、提醒接收人等业务字段。
   - 多家庭切换：依赖 app.globalData.currentFamilyId，在家庭模块中提供切换入口。
