# PAXL模块接口文档

## 轮播图片管理接口

### 1. 上传图片文件

**接口地址：** `POST /api/banner/image/upload`

**接口描述：** 用户选择图片后，先调用此接口上传图片，获取图片存储路径。图片保存在服务器本地，返回相对路径用于后续保存轮播图时使用。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 图片文件，支持格式：jpg、jpeg、png、gif、bmp、webp，最大5MB |

**请求示例：**

```http
POST /api/banner/image/upload HTTP/1.1
Content-Type: multipart/form-data

file: [图片文件]
```

**响应示例：**

```json
"2026/01/05/550e8400e29b41d4a716446655440000.jpg"
```

**响应说明：**
- 返回图片的相对路径，格式：`yyyy/MM/dd/uuid.扩展名`
- 此路径用于后续保存轮播图时使用

**错误响应：**

| HTTP状态码 | 说明 |
|-----------|------|
| 400 | 图片文件为空、格式不支持或大小超限 |
| 500 | 服务器内部错误 |

---

### 2. 保存轮播图片

**接口地址：** `POST /api/banner/save`

**接口描述：** 用户上传图片后，调用此接口保存图片信息到数据库。此接口只保存图片元素信息，不包含图片内容。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| imagePath | String | 是 | 图片存储路径（相对路径），从上传图片接口获取 |
| position | Integer | 否 | 图片位置，用于排序，数字越小越靠前，默认为0 |
| title | String | 否 | 图片标题 |
| description | String | 否 | 图片描述 |
| linkUrl | String | 否 | 点击跳转链接 |
| status | Integer | 否 | 状态：0-禁用，1-启用，默认为1 |

**请求示例：**

```json
{
  "imagePath": "2026/01/05/550e8400e29b41d4a716446655440000.jpg",
  "position": 1,
  "title": "首页轮播图1",
  "description": "这是第一张轮播图",
  "linkUrl": "https://example.com",
  "status": 1
}
```

**响应示例：**

```json
"保存轮播图片成功"
```

**错误响应：**

| HTTP状态码 | 说明 |
|-----------|------|
| 400 | 轮播图片信息为空或图片路径为空 |
| 500 | 服务器内部错误 |

---

### 3. 删除轮播图片

**接口地址：** `POST /api/banner/delete`

**接口描述：** 删除指定的轮播图片，采用逻辑删除方式，同时会删除关联的评论。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bannerId | Long | 是 | 轮播图ID，业务编号 |

**请求示例：**

```http
POST /api/banner/delete?bannerId=1 HTTP/1.1
```

**响应示例：**

```json
"删除轮播图片成功"
```

**错误响应：**

| HTTP状态码 | 说明 |
|-----------|------|
| 400 | 轮播图ID不合法或轮播图片不存在 |
| 500 | 服务器内部错误 |

---

### 4. 查询轮播图片列表

**接口地址：** `GET /api/banner/list`

**接口描述：** 查询所有启用的轮播图片，返回图片元素信息，不包含图片内容。防止一次调用返回多张图片导致报文过大。

**请求参数：** 无

**请求示例：**

```http
GET /api/banner/list HTTP/1.1
```

**响应示例：**

```json
[
  {
    "id": 1,
    "bannerId": 1,
    "imagePath": "2026/01/05/550e8400e29b41d4a716446655440000.jpg",
    "position": 1,
    "title": "首页轮播图1",
    "description": "这是第一张轮播图",
    "linkUrl": "https://example.com",
    "status": 1,
    "deleted": 0,
    "createTime": "2026-01-05T10:00:00",
    "updateTime": "2026-01-05T10:00:00"
  },
  {
    "id": 2,
    "bannerId": 2,
    "imagePath": "2026/01/05/660f9511f30c52e5b827557557766551111.jpg",
    "position": 2,
    "title": "首页轮播图2",
    "description": "这是第二张轮播图",
    "linkUrl": null,
    "status": 1,
    "deleted": 0,
    "createTime": "2026-01-05T11:00:00",
    "updateTime": "2026-01-05T11:00:00"
  }
]
```

**响应说明：**
- 返回的列表按position字段升序排序，position相同则按创建时间倒序
- 只返回status=1且deleted=0的轮播图片
- 图片内容需要通过查看图片接口单独获取

---

### 5. 查看轮播图片

**接口地址：** `GET /api/banner/image/view`

**接口描述：** 根据图片相对路径返回图片流，支持懒加载。前端在轮播图列表中，先获取轮播图信息（包含图片路径），然后逐个调用此接口加载图片。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| path | String | 是 | 图片相对路径，从轮播图信息中的imagePath字段获取 |

**请求示例：**

```http
GET /api/banner/image/view?path=2026/01/05/550e8400e29b41d4a716446655440000.jpg HTTP/1.1
```

**响应说明：**
- 返回图片的字节流
- Content-Type根据图片格式自动设置（image/jpeg、image/png等）
- 设置缓存头，提高性能（max-age=3600）

**错误响应：**

| HTTP状态码 | 说明 |
|-----------|------|
| 400 | 图片路径为空或图片不存在 |
| 500 | 服务器内部错误 |

---

## 评论管理接口

### 6. 保存评论

**接口地址：** `POST /api/banner/comment/save`

**接口描述：** 保存轮播图片的评论信息。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bannerId | Long | 是 | 关联的轮播图ID |
| content | String | 是 | 评论内容 |
| userName | String | 否 | 评论用户名称 |
| status | Integer | 否 | 状态：0-隐藏，1-显示，默认为1 |

**请求示例：**

```json
{
  "bannerId": 1,
  "content": "这张图片很好看！",
  "userName": "张三",
  "status": 1
}
```

**响应示例：**

```json
"保存评论成功"
```

**错误响应：**

| HTTP状态码 | 说明 |
|-----------|------|
| 400 | 评论信息为空、轮播图ID不能为空或评论内容不能为空 |
| 500 | 服务器内部错误 |

---

### 7. 查询评论列表

**接口地址：** `GET /api/banner/comment/list`

**接口描述：** 根据轮播图ID查询该轮播图的所有评论。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bannerId | Long | 是 | 轮播图ID |

**请求示例：**

```http
GET /api/banner/comment/list?bannerId=1 HTTP/1.1
```

**响应示例：**

```json
[
  {
    "id": 1,
    "commentId": 1,
    "bannerId": 1,
    "userName": "张三",
    "content": "这张图片很好看！",
    "status": 1,
    "deleted": 0,
    "createTime": "2026-01-05T12:00:00",
    "updateTime": "2026-01-05T12:00:00"
  },
  {
    "id": 2,
    "commentId": 2,
    "bannerId": 1,
    "userName": "李四",
    "content": "我也觉得不错",
    "status": 1,
    "deleted": 0,
    "createTime": "2026-01-05T13:00:00",
    "updateTime": "2026-01-05T13:00:00"
  }
]
```

**响应说明：**
- 返回的列表按创建时间倒序排序
- 只返回deleted=0的评论

**错误响应：**

| HTTP状态码 | 说明 |
|-----------|------|
| 400 | 轮播图ID不合法 |
| 500 | 服务器内部错误 |

---

## 接口使用流程说明

### 上传轮播图片流程

1. **上传图片文件**
   - 调用接口：`POST /api/banner/image/upload`
   - 上传图片文件，获取图片相对路径

2. **保存轮播图片**
   - 调用接口：`POST /api/banner/save`
   - 传入图片路径和其他信息，保存到数据库

### 查询轮播图片流程

1. **获取轮播图片列表**
   - 调用接口：`GET /api/banner/list`
   - 获取所有启用的轮播图片元素信息（不包含图片内容）

2. **查看具体图片**
   - 调用接口：`GET /api/banner/image/view?path={imagePath}`
   - 根据列表中的imagePath字段，逐个加载图片内容

### 评论功能流程

1. **保存评论**
   - 调用接口：`POST /api/banner/comment/save`
   - 传入轮播图ID和评论内容

2. **查询评论**
   - 调用接口：`GET /api/banner/comment/list?bannerId={bannerId}`
   - 获取指定轮播图的所有评论

---

## 数据库表结构

### t_banner（轮播图片表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID，自增 |
| banner_id | BIGINT | 轮播图业务ID，唯一索引 |
| image_path | VARCHAR(500) | 图片存储路径（相对路径） |
| position | INT | 图片位置，用于排序 |
| title | VARCHAR(200) | 图片标题 |
| description | VARCHAR(500) | 图片描述 |
| link_url | VARCHAR(500) | 点击跳转链接 |
| status | TINYINT | 状态：0-禁用，1-启用 |
| deleted | TINYINT | 是否删除：0-未删除，1-已删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### t_banner_comment（轮播图评论表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID，自增 |
| comment_id | BIGINT | 评论业务ID，唯一索引 |
| banner_id | BIGINT | 关联的轮播图ID |
| user_name | VARCHAR(100) | 评论用户名称 |
| content | TEXT | 评论内容 |
| status | TINYINT | 状态：0-隐藏，1-显示 |
| deleted | TINYINT | 是否删除：0-未删除，1-已删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## 注意事项

1. **图片上传限制**
   - 支持的图片格式：jpg、jpeg、png、gif、bmp、webp
   - 最大文件大小：5MB（5242880字节）

2. **图片存储**
   - 图片保存在服务器本地，路径配置在`application.properties`中的`paxl.image.upload-path`
   - 图片路径格式：`yyyy/MM/dd/uuid.扩展名`

3. **性能优化**
   - 查询轮播图片列表时，只返回图片元素信息，不包含图片内容
   - 图片内容通过单独的接口按需加载，避免一次返回多张图片导致报文过大

4. **删除操作**
   - 删除轮播图片采用逻辑删除（deleted=1），不会物理删除数据
   - 删除轮播图片时，会同时删除关联的评论

