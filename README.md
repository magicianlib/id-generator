# uniqueseq-generater

全局唯一 ID 序列生成器, 基于号段(segment)模式实现。服务为每个业务维度维护一段连续可发的 ID 区间, 业务方每次取号直接从内存缓冲中获取, 仅在号段耗尽时才访问数据库申请新号段, 以极低的数据库压力支撑高频发号。

## 核心特性

- **号段模式发号**: 数据库仅记录每个业务维度的已分配最大值与步阶, 取号走内存缓冲, 数据库交互频次约为步阶分之一
- **双键定位**: 以业务组(bizGroup) + 业务名(bizTag) 唯一确定一个序列, 支持多业务组隔离管理
- **并发安全**: 数据库侧带原值校验的步阶推进, 多实例部署下天然保证区间不重叠; 业务组与业务名组合建唯一索引, 防止重复申请
- **余量预取**: 每个序列的缓冲区维持在安全下限与上限之间, 余量不足时异步补充, 避免取号请求阻塞在数据库上
- **动态感知**: 定时同步已申请序列, 新申请的序列自动进入发号缓存, 废弃的序列自动移除

## 表结构

建表脚本位于 [script-mysql.sql](src/main/resources/script-mysql.sql), 核心表为 `id_segment`:

| 列 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 自增主键 |
| biz_group | varchar(64) | 业务组, 与业务名组合唯一, 默认空串 |
| biz_tag | varchar(64) | 业务名 |
| current_max_id | bigint unsigned | 当前已分配出去的最大 ID 值 |
| step | bigint unsigned | 步阶, 每次申请新号段的区间长度, 默认 1000 |
| description | varchar(500) | 备注说明 |
| created_at / updated_at | datetime | 创建与更新时间(UTC) |

取号推进逻辑: 新号段区间为 (已分配最大值, 已分配最大值 + 步阶], 推进成功后已分配最大值增加一个步阶。

## 快速开始

前置条件: JDK 8+, Maven, 本地 MySQL。

1. 执行建表脚本创建库与表:

```bash
mysql -uroot -p < src/main/resources/script-mysql.sql
```

2. 按需修改 MySQL 连接配置, 见 [application-mysql.properties](src/main/resources/application-mysql.properties), 默认连接本机 3306 端口的 `id_generator_db` 库

3. 启动服务:

```bash
mvn spring-boot:run
```

## API 说明

所有接口均为 POST, 请求与响应均为 JSON, 服务地址默认 `http://localhost:8080`。

响应统一结构: `code` 为 0 表示成功, 非 0 表示失败; `message` 为提示信息; `data` 为业务数据。

### 申请新序列

`POST /api/idSegment/apply`

```bash
$ curl \
--request POST \
http://localhost:8080/api/idSegment/apply \
--header 'Content-Type: application/json' \
-d '{"bizGroup":"order","bizTag":"main","currentMaxId":10000,"step":1000,"description":"订单主表ID"}'
```

`currentMaxId` 与 `step` 可省略, 缺省时使用配置的默认初始值(10000)与默认步阶(1000)。同名序列已存在时幂等返回, 不会重复创建。新申请的序列会在一个同步周期(默认 10 秒)后进入发号缓存。

### 取 1 个 ID

`POST /api/idSegment/take`

```bash
$ curl \
--request POST \
http://localhost:8080/api/idSegment/take \
--header 'Content-Type: application/json' \
-d '{"bizGroup":"order","bizTag":"main"}'
```

### 取 N 个 ID

`POST /api/idSegment/take/{n}`, n 为本次申请的 ID 数量, 必须大于 0:

```bash
$ curl \
--request POST \
http://localhost:8080/api/idSegment/take/10 \
--header 'Content-Type: application/json' \
-d '{"bizGroup":"order","bizTag":"main"}'
```

## 配置项

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| id.segment.default.init_id | 10000 | 申请新序列时缺省的初始已分配最大值 |
| id.segment.default.step | 1000 | 申请新序列时缺省的步阶 |

## 许可证

[MIT](LICENSE)
