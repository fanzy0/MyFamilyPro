# 七、提醒任务调度与到期判断 — 后端设计方案

> 本文档对应开发计划「七、提醒任务调度与到期判断」，依赖前置：
> - 「五」`mf_event` 表结构与 `EventDO` 已实现
> - 「六」`mf_event` 中提醒配置字段（`remind_enabled / remind_advance_days / remind_target / remind_user_id`）已实现
>
> 约束与开发规范遵循：[MyFamilyPro/.cursor/rules/develop.mdc](MyFamilyPro/.cursor/rules/develop.mdc)

---

## 1. 整体思路与核心流程

### 1.1 设计思路确认

| 用户想法 | 设计结论 |
|---|---|
| 每天定时任务扫描所有事件 | ✅ 采用 Spring `@Scheduled` 定时任务，每天固定时间（建议 07:00）执行一次 |
| 根据阳历/农历 + 提前天数判断是否应提醒 | ✅ 计算每个事件"当年应提醒日期"，与当天比较 |
| 命中则为每个提醒人生成一条记录 | ✅ 新建 `mf_remind_log` 表，每条记录对应「一个用户 × 一个事件 × 一年」 |
| 记录支持用户关闭或系统关闭 | ✅ `status` 区分 `PENDING / READ / DONE / CLOSED_BY_USER / CLOSED_BY_SYSTEM` |
| 用户可标记"已完成"并回溯历年记录 | ✅ 新增 `DONE` 状态；新增 history 查询接口，用于事项详情页展示历年完成情况 |
| 事件过期后系统自动关闭未处理记录 | ✅ 定时任务同步执行过期扫描，不单独起任务 |
| 家庭主页悬浮图标展示待处理提醒 | ✅ 新增 `GET /api/remind/count` + `GET /api/remind/active` |
| 点击后可查看、完成或关闭 | ✅ 新增 `POST /api/remind/done` + `POST /api/remind/close` |

### 1.2 状态机说明

```
            ┌──────────────────────────────────┐
            │             PENDING              │  ← 定时任务生成，初始状态
            │     （已生成，用户未查看）          │
            └───────────────┬──────────────────┘
                            │ 用户展开浮层（自动）
                            ▼
            ┌──────────────────────────────────┐
            │              READ                │  ← 用户已查看，未操作
            └──────┬───────────────┬───────────┘
                   │               │
         用户点击   │               │ 用户点击
         「已完成」 │               │ 「关闭/忽略」
                   ▼               ▼
              ┌─────────┐    ┌──────────────┐
              │  DONE   │    │CLOSED_BY_USER│
              └─────────┘    └──────────────┘

            另：PENDING/READ 在 event_date 过后，
            若用户未操作，定时任务将状态改为：
                   ▼
            ┌──────────────────┐
            │ CLOSED_BY_SYSTEM │
            └──────────────────┘
```

**关键语义区分**：

| 状态 | 语义 | 是否计入历年完成 |
|---|---|---|
| `PENDING` | 提醒已生成，未查看 | 否（进行中） |
| `READ` | 用户已看，待处理 | 否（进行中） |
| `DONE` | 用户主动标记已完成（例如已送礼、已体检） | **是**（核心回溯状态） |
| `CLOSED_BY_USER` | 用户忽略本次提醒（不代表事项完成） | 否（不计入完成） |
| `CLOSED_BY_SYSTEM` | 事件已过期，系统自动关闭 | 否（未处理/过期） |

> `DONE` 与 `CLOSED_BY_USER` 的区分很重要：前者表示"我做了这件事"，后者表示"我知道了但不想处理"。历年完成情况只统计 `DONE` 记录。

### 1.3 核心流程图

```
每天 07:00 定时任务启动
         │
         ▼
  查询所有 remind_enabled=1 且有效的 mf_event 记录
         │
         ├─ 对每条 event：
         │     ├─ 计算 eventDateThisYear（当年实际日期，农历需转阳历）
         │     ├─ 计算 remindDateThisYear = eventDateThisYear - remindAdvanceDays
         │     ├─ 判断：today >= remindDateThisYear  &&  today <= eventDateThisYear
         │     │         └─ 是：确定提醒对象（ALL=全体成员 / SPECIFIC=指定人）
         │     │               └─ 对每个目标用户：去重检查 mf_remind_log
         │     │                     └─ 不存在同年记录 → 插入 PENDING
         │     └─ 判断：today > eventDateThisYear
         │               └─ 扫描该 event 当年的 PENDING/READ 记录 → 批量改为 CLOSED_BY_SYSTEM
         │                  （DONE / CLOSED_BY_USER 不受影响，已是终态）
         ▼
        完成，记录任务日志
```

### 1.4 关键设计决策

1. **提醒周期固定为年**，每年重新生成一次提醒记录（`trigger_year` 区分年份）。
2. **去重保证**：唯一索引 `(event_id, user_id, trigger_year)` 保证每年每人只有一条记录。
3. **容错窗口**：条件用窗口 `today >= remindDate && today <= eventDate`，避免单次任务跳过后无法补跑。
4. **终态不可逆**：`DONE / CLOSED_BY_USER / CLOSED_BY_SYSTEM` 均为终态，不允许再流转。
5. **农历转换**：引入第三方工具类，不自行实现，见 §4.3。
6. **`action_time` 统一记录终态时间**：不区分 done_time / close_time，用 `status` 区分语义，表结构更简洁。

---

## 2. 数据库表设计

### 2.1 新增表：`mf_remind_log`（提醒记录）

> 位置：`MyFamilyBack/paxl/sql/paxl_init.sql`（末尾追加）

**字段说明**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT NOT NULL AUTO_INCREMENT | 主键 |
| `event_id` | BIGINT NOT NULL | 关联 `mf_event.id` |
| `family_id` | BIGINT NOT NULL | 冗余字段，便于按家庭查询，避免多表 JOIN |
| `user_id` | BIGINT NOT NULL | 被提醒的用户 ID（`mf_user.id`） |
| `trigger_year` | INT NOT NULL | 本条记录对应的年份（如 2026）；去重用 |
| `event_date` | DATE NOT NULL | 当年事件实际发生日期（已转换为阳历）；过期判断依据 |
| `remind_date` | DATE NOT NULL | 当年应开始提醒日期（`event_date - remind_advance_days`） |
| `status` | VARCHAR(20) NOT NULL DEFAULT 'PENDING' | 状态枚举，见下表 |
| `action_time` | DATETIME DEFAULT NULL | 用户或系统产生终态动作的时间（DONE/关闭时写入） |
| `create_time` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| `update_time` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |

> 与旧设计的差异说明：
> - 原 `close_time` + `close_type` 两字段 → 合并为 `action_time` 一个字段（`status` 本身已区分关闭类型，`close_type` 冗余）
> - 新增 `DONE` 状态，`action_time` 同时用于记录"完成时间"

**status 枚举约定**：

| 值 | 说明 | 终态 |
|---|---|---|
| `PENDING` | 已生成、用户未查看/未操作 | 否 |
| `READ` | 用户已查看（展开浮层时自动标记） | 否 |
| `DONE` | 用户主动标记"已完成"（计入历年完成记录） | **是** |
| `CLOSED_BY_USER` | 用户主动关闭/忽略本次提醒 | **是** |
| `CLOSED_BY_SYSTEM` | 系统在 event_date 过后自动关闭 | **是** |

**索引设计**：

```sql
PRIMARY KEY (`id`)
UNIQUE KEY `uk_event_user_year`   (`event_id`, `user_id`, `trigger_year`)
KEY `idx_user_status`             (`user_id`, `status`)
KEY `idx_family_status`           (`family_id`, `status`)
KEY `idx_event_date`              (`event_date`)
KEY `idx_event_id_status`         (`event_id`, `status`)   -- 历年完成查询
```

> `idx_event_id_status` 用于 `selectDoneHistoryByEvent(eventId)` 查询，按 `trigger_year DESC` 排序展示历年完成情况。

---

## 3. 后端代码落地清单

### 3.1 新增文件清单

```
src/main/java/com/my/family/paxl/
├── domain/
│   ├── entity/
│   │   └── RemindLogDO.java              ★ 新增：提醒记录实体
│   └── vo/
│       ├── RemindLogVO.java              ★ 新增：活跃提醒响应 VO（浮层用）
│       ├── RemindHistoryVO.java          ★ 新增：历年完成记录 VO（事项详情用）
│       ├── RemindActionRequest.java      ★ 新增：提醒操作请求体（完成/关闭共用）
├── mapper/
│   └── RemindLogMapper.java              ★ 新增：继承 BaseMapper<RemindLogDO>
├── service/
│   ├── RemindService.java                ★ 新增：提醒业务接口
│   └── impl/
│       └── RemindServiceImpl.java        ★ 新增：提醒业务实现
├── controller/
│   └── RemindController.java             ★ 新增：提醒相关 REST 接口
├── scheduled/
│   └── RemindScanJob.java                ★ 新增：定时扫描任务（独立 package）
└── util/
    └── LunarCalendarUtil.java            ★ 新增：农历转换工具类

src/main/resources/mapper/
└── RemindLogMapper.xml                   ★ 新增：手写 SQL

sql/
└── paxl_init.sql                         ▲ 修改：末尾追加 mf_remind_log 建表 SQL
```

### 3.2 修改文件清单

| 文件 | 修改点 |
|---|---|
| `PaxlApplication.java` | 类上新增 `@EnableScheduling` 注解 |
| `pom.xml` | 新增农历转换库依赖（见 §4.3） |
| `config/WebMvcConfig.java` | **无需修改**（`/api/remind/**` 自动被 `AuthInterceptor` 拦截） |

---

## 4. 各模块详细设计

### 4.1 `RemindLogDO.java`（新建）

**位置**：`domain/entity/RemindLogDO.java`

**规范**：与 `EventDO` 一致，显式 getter/setter，不使用 `@Data`；加 `@TableName("mf_remind_log")`。

**字段映射**：完整映射 §2.1 所有列，重点说明：

- `status` 使用 `String` 类型，定义常量：

  ```
  STATUS_PENDING          = "PENDING"
  STATUS_READ             = "READ"
  STATUS_DONE             = "DONE"
  STATUS_CLOSED_BY_USER   = "CLOSED_BY_USER"
  STATUS_CLOSED_BY_SYSTEM = "CLOSED_BY_SYSTEM"
  ```

- `eventDate` / `remindDate` 使用 `LocalDate`（非 `LocalDateTime`）
- `actionTime` 使用 `LocalDateTime`，终态时写入，非终态为 null

---

### 4.2 `RemindLogMapper.java` + `RemindLogMapper.xml`（新建）

**位置**：
- `mapper/RemindLogMapper.java`
- `resources/mapper/RemindLogMapper.xml`

**接口方法设计**：

| 方法 | SQL 说明 |
|---|---|
| `int insertIgnore(RemindLogDO log)` | INSERT IGNORE INTO，唯一索引冲突时静默忽略 |
| `List<RemindLogDO> selectExpiredPending(LocalDate beforeDate)` | 查 `event_date < beforeDate AND status IN ('PENDING','READ')` |
| `int batchCloseBySys(List<Long> ids, LocalDateTime actionTime)` | 批量将指定 id 更新为 `CLOSED_BY_SYSTEM`，写入 `action_time` |
| `List<RemindLogVO> selectActiveByUserAndFamily(Long userId, Long familyId)` | 查 `PENDING/READ`，JOIN `mf_event` 补充展示字段，按 `event_date ASC` 排序 |
| `int countActiveByUserAndFamily(Long userId, Long familyId)` | 统计 `PENDING/READ` 数量（红点用） |
| `int markRead(Long id, Long userId)` | PENDING → READ（用户展开浮层时批量/单条调用） |
| `int markDone(Long id, Long userId, LocalDateTime actionTime)` | READ/PENDING → DONE，写入 `action_time`；同时校验 `user_id` 防越权 |
| `int closeByUser(Long id, Long userId, LocalDateTime actionTime)` | READ/PENDING → CLOSED_BY_USER，写入 `action_time` |
| `List<RemindHistoryVO> selectHistoryByEvent(Long eventId, Long userId)` | 查该事件该用户所有历史记录（含终态），按 `trigger_year DESC` 排序，用于历年回溯 |

> 说明：
> - `markDone` / `closeByUser` 的 WHERE 条件需包含 `status IN ('PENDING','READ')` 以防止对已终态记录重复操作，操作影响行数为 0 时 Service 层返回 404 或忽略（取决于业务判断）。
> - `selectHistoryByEvent` 查询范围为**该事件**的**所有年份**记录，包含全部状态。前端可用于展示"历年提醒情况"。
> - `selectActiveByUserAndFamily` 的 JOIN 字段：`title / category / description`（来自 `mf_event`）。

---

### 4.3 `LunarCalendarUtil.java`（新建）

**位置**：`util/LunarCalendarUtil.java`

**作用**：封装第三方农历库，对外提供统一接口，屏蔽库 API 细节。

**对外方法**：

```
LocalDate lunarToSolar(int year, int lunarMonth, int lunarDay)
    将指定年份的农历月/日转换为对应的阳历 LocalDate
    参数：year=阳历年份，lunarMonth/lunarDay=农历月日
    返回：阳历日期（若该农历日期在当年不存在，返回最近有效日）
```

**农历库选型**（在 `pom.xml` 中添加）：

推荐 `lunar-java`（`io.github.iceyangcc:lunar-java`），体积小且 Java 8 兼容。

```xml
<dependency>
    <groupId>io.github.iceyangcc</groupId>
    <artifactId>lunar-java</artifactId>
    <version>（实现时查 Maven Central 最新稳定版）</version>
</dependency>
```

> 备选：`cn.hutool:hutool-core`（含 `ChineseDate`，但体积较大）。

---

### 4.4 `RemindScanJob.java`（新建）

**位置**：`scheduled/RemindScanJob.java`

**注解**：`@Component` + `@Slf4j`；定时方法用 `@Scheduled(cron = "0 0 7 * * ?")`（每天 07:00）。

**注入依赖**：`EventMapper`、`FamilyMemberMapper`、`RemindLogMapper`、`LunarCalendarUtil`。

**核心方法**：

```
void scanAndGenerate()     // 主入口，@Scheduled 触发
void generateRemindLogs()  // 生成新的提醒记录
void closeExpiredLogs()    // 关闭过期的 PENDING/READ 记录（不影响 DONE/CLOSED_BY_USER）
```

**`generateRemindLogs()` 步骤**：

1. 查询所有 `remind_enabled=1 AND status=0 AND deleted=0` 的事件。
2. 取 `currentYear / today`。
3. 对每条 `EventDO`：
   - a. 计算 `eventDateThisYear`：阳历直接构造，农历调 `LunarCalendarUtil.lunarToSolar`；遇非法日期（如 2 月 30 日）取月末最后一天。
   - b. 计算 `remindDateThisYear = eventDateThisYear.minusDays(remindAdvanceDays)`。
   - c. 条件：`today >= remindDateThisYear && today <= eventDateThisYear` 不满足则跳过。
   - d. 确定提醒人：`ALL` → 查家庭全体 APPROVED 成员；`SPECIFIC` → 取 `remindUserId`。
   - e. 对每个 `userId`：构造 `RemindLogDO`（status=PENDING），调 `insertIgnore` 去重插入。
4. 逐条记录 DEBUG 日志，异常捕获后记 ERROR 并继续（不中断整体任务）。

**`closeExpiredLogs()` 步骤**：

1. 调 `selectExpiredPending(today)` 查询需关闭记录（`event_date < today AND status IN (PENDING,READ)`）。
2. 分批（每批 100 条）调 `batchCloseBySys(ids, now())`。
3. 记录关闭条数日志。

> **注意**：`DONE` 和 `CLOSED_BY_USER` 已是终态，过期扫描不会触碰它们，历年完成记录完整保留。

---

### 4.5 `RemindService.java` + `RemindServiceImpl.java`（新建）

**位置**：`service/RemindService.java`、`service/impl/RemindServiceImpl.java`

**接口方法设计**：

```
/**
 * 统计当前用户在指定家庭的活跃提醒数（PENDING+READ）
 * 用于家庭主页红点展示
 */
int countActiveReminds(Long currentUserId, Long familyId);

/**
 * 查询活跃提醒列表（PENDING+READ）
 * 调用时同步将 PENDING → READ（标记已读）
 */
List<RemindLogVO> listActiveReminds(Long currentUserId, Long familyId);

/**
 * 用户标记"已完成"
 * 终态：DONE；写入 action_time；校验 user_id 防越权
 */
void doneRemind(Long currentUserId, Long remindLogId);

/**
 * 用户关闭/忽略一条提醒
 * 终态：CLOSED_BY_USER；写入 action_time；校验 user_id 防越权
 */
void closeRemind(Long currentUserId, Long remindLogId);

/**
 * 查询某事项的历年提醒记录（含所有状态，按 trigger_year DESC）
 * 用于重要事项详情页"历年完成情况"展示
 * 校验：currentUserId 必须是该事项所属家庭的 APPROVED 成员
 */
List<RemindHistoryVO> listRemindHistory(Long currentUserId, Long eventId);
```

**校验规范**：

- `countActiveReminds / listActiveReminds`：校验 `currentUserId` 是 `familyId` 的 APPROVED 成员。
- `doneRemind / closeRemind`：先查记录存在性（不存在→404），再校验 `user_id == currentUserId`（不匹配→403），再校验 `status` 非终态（已是终态→返回 409 或直接 ignore，具体策略在实现时决定）。
- `listRemindHistory`：通过 `eventId` 查到 `familyId`，再校验成员身份。

---

### 4.6 `RemindController.java`（新建）

**位置**：`controller/RemindController.java`

**注解**：`@RestController`、`@RequestMapping("/api/remind")`、`@Slf4j`

**接口设计**：

| 方法 | 路径 | 参数 | 响应 | 说明 |
|---|---|---|---|---|
| `GET` | `/api/remind/count` | Query: `familyId` | `Integer` | 家庭主页红点数量（PENDING+READ） |
| `GET` | `/api/remind/active` | Query: `familyId` | `List<RemindLogVO>` | 活跃提醒列表，同时标记已读 |
| `POST` | `/api/remind/done` | Body: `RemindActionRequest {remindLogId}` | `String("OK")` | 标记已完成 |
| `POST` | `/api/remind/close` | Body: `RemindActionRequest {remindLogId}` | `String("OK")` | 关闭/忽略提醒 |
| `GET` | `/api/remind/history` | Query: `eventId` | `List<RemindHistoryVO>` | 某事项的历年提醒记录（含全部状态） |

**错误响应约定**：

| 状态码 | 场景 |
|---|---|
| 400 | 参数缺失/非法 |
| 403 | 非家庭成员 / 非该提醒的用户 |
| 404 | 提醒记录不存在 |
| 409 | 该记录已是终态，无法再操作 |
| 500 | 服务端异常 |

---

### 4.7 `RemindLogVO.java`（新建，活跃提醒用）

**位置**：`domain/vo/RemindLogVO.java`

**规范**：Lombok `@Data`。

**字段**：

```
Long      remindLogId         // 操作时传回（done/close 时使用）
Long      eventId
String    title               // 事项名称（JOIN mf_event）
String    category            // 类别（JOIN mf_event）
String    status              // PENDING / READ
LocalDate eventDate           // 实际事件日期（阳历，用于"还有X天"计算）
LocalDate remindDate          // 提醒开始日期
Integer   remindAdvanceDays   // 提前天数（展示"提前X天提醒"）
```

---

### 4.8 `RemindHistoryVO.java`（新建，历年回溯用）

**位置**：`domain/vo/RemindHistoryVO.java`

**规范**：Lombok `@Data`。

**字段**：

```
Long          remindLogId     // 记录 ID
Integer       triggerYear     // 年份（如 2024、2025、2026）
LocalDate     eventDate       // 当年事件日期（阳历）
String        status          // DONE / CLOSED_BY_USER / CLOSED_BY_SYSTEM / PENDING / READ
LocalDateTime actionTime      // 操作时间（DONE 时即"完成时间"，关闭时即"关闭时间"，进行中为 null）
```

> 前端展示示例：
>
> ```
> 2026年  🔔 提醒中  (还有 3 天)
> 2025年  ✅ 已完成  (2025-03-15 完成)
> 2024年  ✅ 已完成  (2024-03-16 完成)
> 2023年  ⏹ 系统关闭（未处理，已过期）
> 2022年  🚫 已忽略
> ```

---

### 4.9 `RemindActionRequest.java`（新建，done/close 共用）

**位置**：`domain/vo/RemindActionRequest.java`

**字段**：

```
Long remindLogId   // 必填，要操作的提醒记录 ID
```

> `done` 与 `close` 语义不同，各有独立接口（`/done` 和 `/close`），使用同一请求体结构，后续如需附加备注字段（如完成备注），可扩展此类。

---

## 5. 与前端交互约定

> 前端详细设计输出到：`MyFamilyPro/DesignMd/event/rWeb.md`（待前端设计阶段输出）

### 5.1 家庭主页浮层（家庭首页 Tab）

1. `onShow / _refreshFamilyData` 完成后调用 `GET /api/remind/count?familyId=xxx`。
2. count > 0：展示悬浮图标（📋 + 红点数字）。
3. 用户点击悬浮图标：调用 `GET /api/remind/active?familyId=xxx`，展开浮层列表。
4. 浮层每条记录展示：事项名称、类别 Emoji、事件日期（"还有 X 天" / "今天" / "已过期"）。
5. 每条操作区：「已完成」按钮（→ `/remind/done`）+ 「忽略」按钮（→ `/remind/close`）。
6. 操作成功后从列表移除该条，同步刷新 count。

### 5.2 重要事项详情页（历年完成情况）

> 入口：`pages/event/edit/edit`（或后续独立的 `pages/event/detail/detail` 页面）

1. 进入事项详情时，调用 `GET /api/remind/history?eventId=xxx`。
2. 若历史列表非空，在页面底部展示"历年提醒情况"卡片。
3. 按 `triggerYear DESC` 排列，每行展示：年份、状态图标+文字、操作时间（若有）。
4. 当前年份如有 PENDING/READ 记录，展示"提醒中"状态（带"还有X天"倒计时）。

---

## 6. 开发顺序建议

```
1.  SQL            → 追加 mf_remind_log 建表语句到 paxl_init.sql
2.  pom.xml        → 引入农历库依赖
3.  RemindLogDO    → 实体类（含5个状态常量）
4.  Mapper + XML   → 手写 SQL（重点：insertIgnore / selectHistoryByEvent / selectActiveByUserAndFamily）
5.  LunarUtil      → 工具类，先写单测验证农历→阳历转换
6.  Service        → 业务逻辑（countActive / listActive / done / close / history）
7.  Controller     → 5个接口
8.  RemindScanJob  → 定时任务（放最后，方便用接口手动触发调试）
9.  PaxlApplication → 添加 @EnableScheduling
10. 集成验证       → 手动构造 event → 调 generateRemindLogs() → 查 mf_remind_log
                    → 调 /done → 再调 /history → 验证历年记录
```

---

## 7. 边界情况处理说明

| 边界情况 | 处理策略 |
|---|---|
| 阳历 2 月 29 日（非闰年） | `LocalDate.of(year, 2, 29)` 抛 `DateTimeException`，捕获后取 `2 月 28 日` |
| 农历月份在当年不存在（如闰月） | 委托 `LunarCalendarUtil` 处理，取最近有效日 |
| 年末事件 remindDate 落到上一年 | 首版简化：仅在 `remindDate <= today <= eventDate` 窗口内生成，不跨年 |
| 事件被删除/禁用后仍有 PENDING 记录 | 推荐：删除/禁用 event 时，同步将该 event 的 PENDING/READ 记录关为 CLOSED_BY_SYSTEM（在 EventServiceImpl 中扩展） |
| 用户退出家庭后仍有 PENDING/READ 记录 | 退出时（FamilyService 中），同步关闭该用户在该家庭的所有 PENDING/READ 记录 |
| 对已是终态（DONE/CLOSED_*）的记录再次操作 | Mapper 的 WHERE 条件带 `status IN ('PENDING','READ')`，影响行=0，Service 返回 409 |
| 多实例部署任务并发 | 首版单实例，暂不考虑；扩展时引入 `ShedLock` 或数据库分布式锁 |
| `history` 接口数据量过大 | 首版不做分页（事项通常不超过几十年）；数据量大时可加 `LIMIT 20` |

---

## 8. 文档补充指引

| 文档 | 补充内容 |
|---|---|
| `DesignMd/event/rPlan.md`（本文） | 后端完整设计（已完成） |
| `DesignMd/event/rWeb.md` | 前端浮层提醒 + 历年完成展示设计（待前端阶段输出） |
| `DesignMd/event/ePlan.md` | 末尾追加"Plan 七接口汇总"，与 Plan 五的 `/api/event/**` 并列展示 |
| `MyFamilyBack/paxl/sql/paxl_init.sql` | 追加 `mf_remind_log` 建表 SQL |

---

*后端设计方案结束。*
