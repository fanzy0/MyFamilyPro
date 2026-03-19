# 家庭重要事项接口文档

> 所有接口均需鉴权，请求头必须携带 `X-WX-OPENID`（微信云托管自动注入）

---

## 1. 创建重要事项

### 接口信息
- 接口路径：`/api/event/create`
- 请求方法：POST
- 接口描述：家庭成员创建重要事项，当前用户必须是该家庭的正式成员（join_status=APPROVED）

### 请求参数（Body JSON）

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| familyId | Long | 是 | 所属家庭ID |
| title | String | 是 | 事项名称（最大100字） |
| category | String | 是 | 类别：BIRTHDAY/ANNIVERSARY/HOLIDAY/DOCUMENT_EXPIRY/HEALTH/PAYMENT/OTHER |
| dateType | Integer | 是 | 日期类型：0=阳历，1=农历 |
| month | Integer | 是 | 月份（1-12；dateType=0 表示阳历月；dateType=1 表示农历月） |
| day | Integer | 是 | 日期（1-31；dateType=0 表示阳历日；dateType=1 表示农历日） |
| description | String | 否 | 事项描述/备注（最大500字） |
| remindEnabled | Integer | 否 | 是否开启提醒：0=否，1=是（默认0） |
| remindAdvanceDays | Integer | 否 | 提前几天提醒（默认0=当天提醒） |
| remindTarget | String | 否 | 提醒对象：ALL=全部成员，SPECIFIC=指定某人（默认ALL） |
| remindUserId | Long | 条件必填 | 指定提醒用户ID（remindTarget=SPECIFIC 时必填） |

### 响应参数

同「查询事项详情」响应参数。

### 请求示例

```json
{
  "familyId": 1,
  "title": "妈妈生日",
  "category": "BIRTHDAY",
  "dateType": 1,
  "month": 3,
  "day": 18,
  "description": "记得买蛋糕",
  "remindEnabled": 1,
  "remindAdvanceDays": 3,
  "remindTarget": "ALL"
}
```

### 响应示例

```json
{
  "id": 10,
  "familyId": 1,
  "title": "妈妈生日",
  "category": "BIRTHDAY",
  "dateType": 1,
  "month": 3,
  "day": 18,
  "description": "记得买蛋糕",
  "remindEnabled": 1,
  "remindAdvanceDays": 3,
  "remindTarget": "ALL",
  "remindUserId": null,
  "creatorUserId": 5,
  "creatorNickname": "小明",
  "creatorAvatarUrl": "https://...",
  "createTime": "2026-03-17T10:00:00",
  "updateTime": "2026-03-17T10:00:00",
  "isOwner": true
}
```

### 错误响应

| 状态码 | 场景 |
|--------|------|
| 400 | 参数缺失或格式错误 |
| 403 | 当前用户不是该家庭的正式成员 |
| 500 | 服务器内部错误 |

---

## 2. 查询家庭事项列表

### 接口信息
- 接口路径：`/api/event/list`
- 请求方法：GET
- 接口描述：查询指定家庭的全部重要事项列表，按 month/day 升序排列

### 请求参数（Query）

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| familyId | Long | 是 | 家庭ID |

### 响应参数（数组）

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 事项主键ID |
| familyId | Long | 所属家庭ID |
| title | String | 事项名称 |
| category | String | 类别 |
| dateType | Integer | 日期类型：0=阳历，1=农历 |
| month | Integer | 月份（1-12） |
| day | Integer | 日期（1-31） |
| remindEnabled | Integer | 是否开启提醒 |
| creatorUserId | Long | 创建人用户ID |
| creatorNickname | String | 创建人昵称 |
| createTime | String | 创建时间 |
| isOwner | Boolean | 是否为当前用户创建（true=可删除） |

### 请求示例

```
GET /api/event/list?familyId=1
```

### 响应示例

```json
[
  {
    "id": 10,
    "familyId": 1,
    "title": "妈妈生日",
    "category": "BIRTHDAY",
    "dateType": 1,
    "month": 3,
    "day": 18,
    "remindEnabled": 1,
    "creatorUserId": 5,
    "creatorNickname": "小明",
    "createTime": "2026-03-17T10:00:00",
    "isOwner": true
  },
  {
    "id": 11,
    "familyId": 1,
    "title": "驾照到期",
    "category": "DOCUMENT_EXPIRY",
    "dateType": 0,
    "month": 6,
    "day": 1,
    "remindEnabled": 1,
    "creatorUserId": 6,
    "creatorNickname": "小红",
    "createTime": "2026-03-17T11:00:00",
    "isOwner": false
  }
]
```

### 错误响应

| 状态码 | 场景 |
|--------|------|
| 400 | familyId 为空 |
| 403 | 当前用户不是该家庭的正式成员 |
| 500 | 服务器内部错误 |

---

## 3. 查询事项详情

### 接口信息
- 接口路径：`/api/event/detail`
- 请求方法：GET
- 接口描述：查询单个重要事项的完整详情（含提醒配置）

### 请求参数（Query）

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| eventId | Long | 是 | 事项ID |

### 响应参数

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 事项主键ID |
| familyId | Long | 所属家庭ID |
| title | String | 事项名称 |
| category | String | 类别 |
| dateType | Integer | 日期类型：0=阳历，1=农历 |
| month | Integer | 月份（1-12） |
| day | Integer | 日期（1-31） |
| description | String | 描述/备注 |
| remindEnabled | Integer | 是否开启提醒 |
| remindAdvanceDays | Integer | 提前几天提醒 |
| remindTarget | String | 提醒对象 |
| remindUserId | Long | 指定提醒用户ID |
| creatorUserId | Long | 创建人用户ID |
| creatorNickname | String | 创建人昵称 |
| creatorAvatarUrl | String | 创建人头像 |
| createTime | String | 创建时间 |
| updateTime | String | 更新时间 |
| isOwner | Boolean | 是否为当前用户创建 |

### 请求示例

```
GET /api/event/detail?eventId=10
```

### 错误响应

| 状态码 | 场景 |
|--------|------|
| 400 | eventId 为空 |
| 403 | 当前用户不是该家庭的正式成员 |
| 404 | 事项不存在或已删除 |
| 500 | 服务器内部错误 |

---

## 4. 删除重要事项

### 接口信息
- 接口路径：`/api/event/delete`
- 请求方法：DELETE
- 接口描述：逻辑删除重要事项，**仅创建人可操作**，其他成员返回 403

### 请求参数（Query）

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| eventId | Long | 是 | 事项ID |

### 响应参数

| 类型 | 说明 |
|------|------|
| String | "删除成功" |

### 请求示例

```
DELETE /api/event/delete?eventId=10
```

### 响应示例

```
删除成功
```

### 错误响应

| 状态码 | 场景 |
|--------|------|
| 400 | eventId 为空 |
| 403 | 当前用户不是事项创建人 |
| 404 | 事项不存在或已删除 |
| 500 | 服务器内部错误 |

---

## 类别枚举说明

| 枚举值 | 中文说明 |
|--------|----------|
| BIRTHDAY | 生日 |
| ANNIVERSARY | 纪念日 |
| HOLIDAY | 节假日 |
| DOCUMENT_EXPIRY | 证件到期 |
| HEALTH | 健康体检 |
| PAYMENT | 还款缴费 |
| OTHER | 其他 |
