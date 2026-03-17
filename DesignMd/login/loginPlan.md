二、用户体系与微信一键登录开发步骤规划

【前端请求方案决策：采用方案A（微信云托管）】
项目前端已采用 wx.cloud.callContainer 调用后端（云托管方式），本模块所有开发均以此为基础：
- 前端通过 wx.cloud.callContainer 发起请求，微信云托管平台自动向后端注入 X-WX-OPENID Header。
- 后端从 X-WX-OPENID Header 中获取用户 openid，无需调用微信 auth.code2Session 接口换取 openid。
- 鉴权中间件读取云托管注入的 X-WX-OPENID，加载用户上下文，替代 JWT 方案。
- 已有的 MyFamilyWeb/utils/cloudRequest.js 保持不变，在其基础上做增强封装。

一、后端基础准备（MyFamilyBack）
1. 检查并完善后端工程基础结构（分层：controller / service / repository / domain / config 等）。
2. 无需引入额外 HTTP 客户端依赖（云托管代理了请求，openid 由平台注入，不需要后端主动调微信接口）。

二、数据库与实体模型实现
1. 创建用户表 mf_user，按照 userAndLogin.md 中的首版精简字段实现（不含 has_family、country/province/city/gender 等非首版字段）。
2. 在后端创建对应实体类 / DO（含基础字段、状态枚举）。
3. 创建用户仓储层（Mapper / Repository）：支持按 openid 查询用户、新增用户、更新登录时间等操作。

三、用户业务服务实现
1. 实现用户登录业务服务（UserService）：
   - 从请求 Header X-WX-OPENID 获取 openid（由云托管平台注入，后端直接信任）；
   - 根据 openid 查询用户表，不存在则创建新用户（nickname / avatar_url 初始为空）；
   - 更新用户最近登录时间；
   - 预留查询用户已加入家庭列表（familyList）的调用位置，与家庭模块打通后完善。
2. 预留 updateProfile 服务方法（供后续用户设置昵称/头像调用）。

四、登录接口与鉴权中间件
1. 实现登录接口 POST /api/auth/login：
   - 无需接收 code 参数；
   - 后端从 Header 中读取 X-WX-OPENID；
   - 调用用户服务完成查询或新建用户；
   - 返回用户基础信息 + familyList（首版 familyList 为空数组，与家庭模块打通后填充）。
2. 配置统一鉴权拦截器（Interceptor / Filter）：
   - 对所有业务接口（白名单除外）读取请求 Header 中的 X-WX-OPENID；
   - 根据 openid 查询用户，注入 UserContext 线程变量，供业务层获取；
   - 若 X-WX-OPENID 为空，返回 401 统一错误响应；
   - 白名单：/api/auth/login 本身无需鉴权。
3. 统一异常处理：覆盖 openid 为空、用户被禁用等情况，记录脱敏日志。

五、小程序前端基础封装（MyFamilyWeb）
1. 在已有 cloudRequest.js 基础上增强错误处理：
   - 对 401 / 业务错误码做统一处理（展示 toast 提示，必要时跳转登录页）；
   - 对网络错误、服务器 5xx 统一 toast 提示。
2. 设计并实现登录页 / 登录入口：
   - 展示"微信一键登录"按钮；
   - 点击后直接调用后端 POST /api/auth/login（cloudRequest.post，无需先调 wx.login）；
   - 登录成功后将用户信息、familyList 保存到 app.globalData；
   - 根据 familyList 是否为空跳转不同页面：
     - familyList 为空 → 跳转"创建或加入家庭"引导页；
     - familyList 非空 → 跳转家庭主页（多家庭时需先选择激活的家庭）。
3. 在 app.onLaunch 中执行启动时自动登录校验：
   - 调用 GET /api/user/me（X-WX-OPENID 由云托管自动携带）；
   - 成功则更新 app.globalData，进入主流程；
   - 失败（用户不存在 / 被禁用）则清理全局状态，跳转登录页。

六、与后续模块的衔接准备
1. 在 app.globalData 中预留以下字段，供后续家庭模块使用：
   - currentUser：当前用户基础信息；
   - familyList：用户已加入的家庭列表；
   - currentFamilyId：当前激活的家庭 ID（多家庭时用于切换）。
2. 后端用户实体中预留与家庭、事项关联需要的 id 字段，不在本步骤实现具体家庭逻辑。

七、联调与测试
1. 本地搭建可访问的后端环境，在微信开发者工具中模拟云托管注入的 X-WX-OPENID Header（开发态可通过工具自动携带）。
2. 后端接口完成后，先用 Postman 手动添加 X-WX-OPENID Header 自测，验证通过后再开始前后端联调。
3. 使用微信开发者工具真机 / 模拟器验证：
   - 首次登录：新用户创建成功，familyList 为空，跳转"创建或加入家庭"页；
   - 已有用户：正确返回 familyList，跳转家庭主页；
   - X-WX-OPENID 缺失时（本地测试特殊场景）：正确返回 401。
4. 根据测试结果微调接口字段和错误码，为后续"家庭管理"开发做好数据与登录态基础。
