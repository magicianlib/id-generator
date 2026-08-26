# 压测工具

取号服务的本地压测工具集, 两个独立工具各司其职:

| 工具 | 用途 |
| --- | --- |
| ThroughputBench | 吞吐压测: 统计成功/失败/批量不足额/吞吐/平均延迟 |
| StrictBench | 正确性校验: 收集全部发出的号, 全量校验首号衔接、无重复、连续无跳号 |

均为无依赖的单文件 Java 程序(JDK 8+ 直接编译运行), 不属于服务构建的一部分。

## 前置准备

1. 启动服务(按需选择数据库 profile):

```bash
# MySQL(默认)
mvn spring-boot:run

# PostgreSQL
mvn spring-boot:run -Dspring-boot.run.profiles=postgresql
```

2. 申请压测专用序列(按层级逐级申请, 已存在时幂等):

```bash
curl -X POST http://localhost:8080/api/common/applyGroup \
  --header 'Content-Type: application/json' \
  -d '{"bizGroup":"commons","description":"压测业务组"}'

curl -X POST http://localhost:8080/api/common/applyTag \
  --header 'Content-Type: application/json' \
  -d '{"bizGroup":"commons","bizTag":"benchmark","description":"压测序列"}'

curl -X POST http://localhost:8080/api/common/applySegment \
  --header 'Content-Type: application/json' \
  -d '{"bizGroup":"commons","bizTag":"benchmark","description":"压测序列"}'
```

3. 等待一个同步周期(默认 10 秒)让新序列进入发号缓存, 然后编译工具:

```bash
cd benchmark
javac ThroughputBench.java StrictBench.java
```

## 吞吐压测

```bash
# 单号取号, 64 线程持续 30 秒
java -Dhttp.maxConnections=256 ThroughputBench \
  /api/common/takeSegment \
  '{"bizGroup":"commons","bizTag":"benchmark"}' 64 30

# 批量取号, 每请求 10 个号, 并统计批量不足额请求数
java -Dhttp.maxConnections=256 ThroughputBench \
  /api/common/takeSegment/10 \
  '{"bizGroup":"commons","bizTag":"benchmark"}' 64 30 10
```

参数依次为: 接口路径、请求体、并发线程数、持续秒数、期望批量值(可选, 提供时才统计不足额)。

输出示例:

```
path=/api/common/takeSegment threads=64 ok=283856 fail=0 shortBatch=0 qps=14193 meanLatencyMs=4.50
```

- `qps`: 每秒完成请求数
- `meanLatencyMs`: 客户端视角平均延迟
- `shortBatch`: 批量响应返回数量不足期望值的请求数(有界降级, 详见主 README)

## 正确性校验

期望首号的取值分两种情况:

- **服务冷启动**(刚启动或该序列尚未进入发号缓存): 查库内该序列的已分配最大值, 期望首号为其加一:

```bash
# PostgreSQL
psql -h localhost -U admin -d id_generator_db \
  -tAc "select current_max_id + 1 from id_segment where biz_group='commons' and biz_tag='benchmark'"

# MySQL
mysql -uroot -p -e "select current_max_id + 1 from \`id_generator_db\`.\`id_segment\` where biz_group='commons' and biz_tag='benchmark'"
```

- **服务已发过该序列**(缓存中还有预取未发的余量): 库内推进值会超前于已发出的号, 此时不能按库内值算, 应取上一轮压测实际末号加一

然后运行校验(最后一个参数为期望首号):

```bash
java -Dhttp.maxConnections=256 StrictBench \
  /api/common/takeSegment \
  '{"bizGroup":"commons","bizTag":"benchmark"}' 32 15 2420001
```

输出包含逐项校验结论与总判定:

```
first=2420001 (expect 2420001, OK) last=2519533 dup=none OK gaps=0
VERDICT: PASS (首号衔接/无重复/连续无跳号 全部通过)
```

连续多轮校验时, 下一轮的期望首号取上一轮输出的 `last` 加一。

## 压测口径注意事项

- **必须加 `-Dhttp.maxConnections`**: JDK 长连接池默认每主机仅 5 条连接, 不加会把压测结果压在客户端假瓶颈上, 实测会低估约一半
- **同机压测口径**: 压测器与服务端同机时会互相争抢 CPU, 服务端拿不满全部核数, 测得的是该环境下的下限值; 要测服务端独占容量需把压测器放到独立机器, 把源码中的 `localhost:8080` 换成服务机地址
- **吞吐会随线程数出现平台**: 到达服务端饱和后继续加线程只会推高延迟, 建议从 16 线程起按倍数递增观察吞吐平台
- **压测会真实消耗号**: 每百万号对应数据库推进约一千次, 压测前后号段会大幅前进, 属正常现象
