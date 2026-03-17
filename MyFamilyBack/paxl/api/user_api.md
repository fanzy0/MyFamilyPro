# 用户模块接口文档

> 鉴权方式：微信云托管模式。所有业务接口（白名单除外）由后端 AuthInterceptor 从请求 Header 中读取
> `X-WX-OPENID`（微信云托管平台自动注入），前端无需手动传递任何身份凭证。

---

## 1. 用户登录

### 接口信息
- 接口路径：`POST /api/auth/login`
- 请求方法：POST
- 接口描述：用户登录。无需请求体，openid 由微信云托管平台自动注入到 Header X-WX-OPENID。
  根据 openid 查询或创建用户，返回用户基础信息和已加入的家庭列表。
- 鉴权：**无需鉴权**（白名单接口）

### 请求 Header（由云托管平台自动注入，非前端手动传入）
| Header 名       | 类型   | 说明                       |
|----------------|--------|--------------------------|
| X-WX-OPENID    | String | 微信小程序用户唯一标识 openid       |

### 请求参数
无（Body 为空）

### 响应参数
| 参数名               | 类型            | 说明                              |
|--------------------|-----------------|-----------------------------------|
| user               | Object          | 用户基础信息                          |
| user.id            | Long            | 用户主键 ID                          |
| user.nickname      | String          | 昵称，可为空（用户未主动设置时为空）              |
| user.avatarUrl     | String          | 头像地址，可为空                        |
| user.status        | Integer         | 状态：0-正常，1-禁用                    |
| familyList         | Array           | 已加入的家庭列表（首版始终为空数组）              |
| familyList[].familyId   | Long       | 家庭主键 ID                          |
| familyList[].familyName | String     | 家庭名称                              |
| familyList[].role       | String     | 角色：OWNER-户主，MEMBER-成员           |

### 响应示例
```json
{
  "user": {
    "id": 1001,
    "nickname": "",
    "avatarUrl": "",
    "status": 0
  },
  "familyList": []
}
```

### 错误响应
| HTTP 状态码 | code            | 说明             |
|----------|-----------------|----------------|
| 401      | AUTH_FAILED     | X-WX-OPENID 为空 |
| 500      | -               | 服务器内部异常        |

---

## 2. 获取当前用户信息

### 接口信息
- 接口路径：`GET /api/user/me`
- 请求方法：GET
- 接口描述：供小程序启动时校验用户是否存在，并获取最新用户信息。
  X-WX-OPENID 由云托管自动携带，鉴权拦截器处理后将用户信息注入 UserContext。
- 鉴权：**需要鉴权**

### 请求参数
无

### 响应参数
| 参数名          | 类型     | 说明                   |
|--------------|--------|----------------------|
| id           | Long   | 用户主键 ID              |
| nickname     | String | 昵称，可为空               |
| avatarUrl    | String | 头像地址，可为空             |
| status       | Integer | 状态：0-正常，1-禁用        |

### 响应示例
```json
{
  "id": 1001,
  "nickname": "小明",
  "avatarUrl": "https://example.com/avatar.jpg",
  "status": 0
}
```

### 错误响应
| HTTP 状态码 | code           | 说明          |
|----------|----------------|-------------|
| 401      | AUTH_FAILED    | 身份验证失败      |
| 401      | USER_NOT_FOUND | 用户不存在       |
| 403      | USER_DISABLED  | 账号已被禁用      |
| 500      | -              | 服务器内部异常     |

---

## 3. 更新用户基础资料

### 接口信息
- 接口路径：`POST /api/user/updateProfile`
- 请求方法：POST
- 接口描述：用户在个人资料页通过 `type="nickname"` 输入昵称或 `open-type="chooseAvatar"` 选择
  头像后调用。nickname 和 avatarUrl 均为可选，仅更新非空字段。
- 鉴权：**需要鉴权**

### 请求参数（Body，JSON）
| 参数名       | 类型     | 必填  | 说明                       |
|-----------|--------|-----|--------------------------|
| nickname  | String | 可选  | 用户昵称，通过微信 type="nickname" 输入 |
| avatarUrl | String | 可选  | 用户头像地址，通过微信 chooseAvatar 选择 |

> nickname 和 avatarUrl 至少填写一个，否则返回 400。

### 请求示例
```json
{
  "nickname": "小明",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

### 响应参数
与"获取当前用户信息"接口响应结构相同，返回更新后的用户信息。

### 响应示例
```json
{
  "id": 1001,
  "nickname": "小明",
  "avatarUrl": "https://example.com/avatar.jpg",
  "status": 0
}
```

### 错误响应
| HTTP 状态码 | code           | 说明                |
|----------|----------------|-------------------|
| 400      | -              | nickname 和 avatarUrl 均为空 |
| 401      | AUTH_FAILED    | 身份验证失败            |
| 403      | USER_DISABLED  | 账号已被禁用            |
| 500      | -              | 服务器内部异常           |

---

## 附：鉴权拦截器统一错误响应格式

所有鉴权失败的接口均返回以下 JSON 格式：

```json
{
  "code": "AUTH_FAILED",
  "message": "身份验证失败，请重新登录"
}
```

| code           | HTTP 状态码 | 说明           |
|----------------|----------|--------------|
| AUTH_FAILED    | 401      | X-WX-OPENID 为空 |
| USER_NOT_FOUND | 401      | 用户不存在，请先登录   |
| USER_DISABLED  | 403      | 账号已被禁用       |
