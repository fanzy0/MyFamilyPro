# 提醒记录接口文档

> 模块：七、提醒任务调度与到期判断
> 前置依赖：用户已登录（`AuthInterceptor` 鉴权），所有接口均需携带有效 Token

---

## 1. 查询活跃提醒数量（红点用）

### 接口信息
- 接口路径：`GET /api/remind/count`
- 描述：查询当前用户在指定家庭的活跃提醒数量（状态为 PENDING 或 READ），用于家庭主页红点展示

### 请求参数
| 参数名 | 位置 | 类型 | 必填 | 说明 |
|-------|------|------|-----|------|
| familyId | Query | Long | 是 | 家庭ID |

### 响应参数
| 类型 | 说明 |
|------|------|
| Integer | 活跃提醒数量（0 表示无提醒） |

### 响应示例
```json
3
```

### 错误码
| 状态码 | 说明 |
|-------|------|
| 400 | 参数缺失/非法 |
| 403 | 非该家庭成员 |
| 500 | 服务端异常 |

---

## 2. 查询活跃提醒列表（浮层展开）

### 接口信息
- 接口路径：`GET /api/remind/active`
- 描述：查询当前用户在指定家庭的活跃提醒列表；调用时同步将 PENDING → READ（标记已读）

### 请求参数
| 参数名 | 位置 | 类型 | 必填 | 说明 |
|-------|------|------|-----|------|
| familyId | Query | Long | 是 | 家庭ID |

### 响应参数（`List<RemindLogVO>`）
| 参数名 | 类型 | 说明 |
|-------|------|------|
| remindLogId | Long | 提醒记录ID，执行 done/close 时回传 |
| eventId | Long | 关联事项ID |
| title | String | 事项名称 |
| category | String | 事项类别（BIRTHDAY / ANNIVERSARY 等） |
| status | String | 当前状态：PENDING 或 READ |
| eventDate | LocalDate（`yyyy-MM-dd`） | 当年事件实际发生日期（阳历） |
| remindDate | LocalDate（`yyyy-MM-dd`） | 当年应开始提醒日期 |
| remindAdvanceDays | Integer | 提前提醒天数 |

### 响应示例
```json
[
  {
    "remindLogId": 101,
    "eventId": 5,
    "title": "妈妈生日",
    "category": "BIRTHDAY",
    "status": "READ",
    "eventDate": "2026-04-10",
    "remindDate": "2026-04-07",
    "remindAdvanceDays": 3
  }
]
```

---

## 3. 标记提醒为"已完成"

### 接口信息
- 接口路径：`POST /api/remind/done`
- 描述：用户主动标记某条提醒为已完成（DONE），计入历年完成记录；终态不可逆

### 请求参数（JSON Body）
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| remindLogId | Long | 是 | 要操作的提醒记录ID |

### 请求示例
```json
{
  "remindLogId": 101
}
```

### 响应示例
```json
"OK"
```

### 错误码
| 状态码 | 说明 |
|-------|------|
| 400 | remindLogId 为空 |
| 403 | 非该提醒的归属用户 |
| 404 | 提醒记录不存在 |
| 409 | 记录已是终态，无法再操作 |
| 500 | 服务端异常 |

---

## 4. 关闭/忽略提醒

### 接口信息
- 接口路径：`POST /api/remind/close`
- 描述：用户忽略/关闭一条提醒（CLOSED_BY_USER），不计入完成记录；终态不可逆

### 请求参数（JSON Body）
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| remindLogId | Long | 是 | 要操作的提醒记录ID |

### 请求示例
```json
{
  "remindLogId": 101
}
```

### 响应示例
```json
"OK"
```

### 错误码
同 `/api/remind/done`

---

## 5. 查询历年提醒记录

### 接口信息
- 接口路径：`GET /api/remind/history`
- 描述：查询当前用户在某事项下的全部历年提醒记录（含所有状态），按年份降序排列；用于重要事项详情页"历年完成情况"展示

### 请求参数
| 参数名 | 位置 | 类型 | 必填 | 说明 |
|-------|------|------|-----|------|
| eventId | Query | Long | 是 | 事项ID |

### 响应参数（`List<RemindHistoryVO>`）
| 参数名 | 类型 | 说明 |
|-------|------|------|
| remindLogId | Long | 提醒记录ID |
| triggerYear | Integer | 年份（如 2026） |
| eventDate | LocalDate（`yyyy-MM-dd`） | 当年事件实际发生日期（阳历） |
| status | String | 状态：DONE / CLOSED_BY_USER / CLOSED_BY_SYSTEM / PENDING / READ |
| actionTime | LocalDateTime（`yyyy-MM-dd HH:mm:ss`） | 终态时间；进行中为 null |

### 响应示例
```json
[
  {
    "remindLogId": 103,
    "triggerYear": 2026,
    "eventDate": "2026-04-10",
    "status": "PENDING",
    "actionTime": null
  },
  {
    "remindLogId": 87,
    "triggerYear": 2025,
    "eventDate": "2025-04-12",
    "status": "DONE",
    "actionTime": "2025-04-10 09:32:00"
  },
  {
    "remindLogId": 54,
    "triggerYear": 2024,
    "eventDate": "2024-04-20",
    "status": "CLOSED_BY_USER",
    "actionTime": "2024-04-18 20:15:00"
  }
]
```

---

## 附：状态枚举说明

| 状态值 | 语义 | 是否终态 | 计入历年完成 |
|-------|------|---------|------------|
| `PENDING` | 提醒已生成，用户未查看 | 否 | 否 |
| `READ` | 用户已查看（展开浮层时自动标记） | 否 | 否 |
| `DONE` | 用户主动标记已完成 | **是** | **是** |
| `CLOSED_BY_USER` | 用户忽略本次提醒 | **是** | 否 |
| `CLOSED_BY_SYSTEM` | 事件已过期，系统自动关闭 | **是** | 否 |
