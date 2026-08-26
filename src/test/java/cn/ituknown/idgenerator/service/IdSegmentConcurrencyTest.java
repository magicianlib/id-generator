package cn.ituknown.idgenerator.service;

import cn.ituknown.idgenerator.po.IdSegmentPo;
import cn.ituknown.idgenerator.repository.IdGroupRepository;
import cn.ituknown.idgenerator.repository.IdSegmentRepository;
import cn.ituknown.idgenerator.repository.IdTagRepository;
import cn.ituknown.idgenerator.request.ApplyGroupRequest;
import cn.ituknown.idgenerator.request.ApplyIdSegmentRequest;
import cn.ituknown.idgenerator.request.ApplyTagRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发验证: 多线程领取号段区间应不重叠且无缝衔接, 多线程取号应不重复且不跳号, 全程不应触发数据库死锁
 */
@SpringBootTest
class IdSegmentConcurrencyTest {

    private static final String TEST_BIZ_GROUP = "commons";

    /**
     * 区间分配并发验证专用号段, 仅通过存储层直接操作
     */
    private static final String TEST_BIZ_TAG_REPOSITORY = "concurrency-repository";

    /**
     * 取号并发验证专用号段, 仅通过服务层缓存消费
     */
    private static final String TEST_BIZ_TAG_SERVICE = "concurrency-service";

    @Resource
    private CommonIdSegmentService commonIdSegmentService;

    @Resource
    private IdGroupRepository idGroupRepository;

    @Resource
    private IdTagRepository idTagRepository;

    @Resource
    private IdSegmentRepository idSegmentRepository;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 当前连接的数据库产品名, 决定死锁指标读取口径
     */
    private String databaseProductName;

    /**
     * 多线程同时领取号段区间: 区间两两不重叠、彼此无缝衔接、总跨度与库内推进值一致
     */
    @Test
    @Timeout(60)
    void nextSegmentRangeUnderConcurrency() throws Exception {
        int threads = 16;

        applyIfAbsent(TEST_BIZ_TAG_REPOSITORY);
        IdSegmentPo before = idSegmentRepository.get(TEST_BIZ_GROUP, TEST_BIZ_TAG_REPOSITORY);
        long beforeMaxId = before.getCurrentMaxId();
        long step = before.getStep();
        long deadlocksBefore = deadlockCount();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Pair<Long, Long>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return idSegmentRepository.nextSegmentRange(TEST_BIZ_GROUP, TEST_BIZ_TAG_REPOSITORY);
                }));
            }
            ready.await();
            start.countDown();

            // 单个任务带超时上限, 领取卡死即失败
            List<Pair<Long, Long>> ranges = new ArrayList<>();
            for (Future<Pair<Long, Long>> future : futures) {
                ranges.add(future.get(30, TimeUnit.SECONDS));
            }

            assertEquals(threads, ranges.size(), "领取到的区间数量应与线程数一致");
            ranges.sort(Comparator.comparingLong(Pair::getLeft));
            assertEquals(beforeMaxId + 1, ranges.get(0).getLeft(), "首个区间应从已分配最大值之后开始");
            for (int i = 1; i < ranges.size(); i++) {
                assertEquals(ranges.get(i - 1).getRight() + 1, ranges.get(i).getLeft(), "相邻区间应无缝衔接且不重叠");
            }

            IdSegmentPo after = idSegmentRepository.get(TEST_BIZ_GROUP, TEST_BIZ_TAG_REPOSITORY);
            assertEquals(ranges.get(ranges.size() - 1).getRight(), after.getCurrentMaxId(), "库内推进值应与最大区间右端一致");
            assertEquals(threads * step, after.getCurrentMaxId() - beforeMaxId, "总推进跨度应等于线程数乘步阶");
            assertEquals(deadlocksBefore, deadlockCount(), "并发领取期间不应发生数据库死锁");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 多线程混合单个取号与批量取号: 发出的号总数准确、无重复、连续无跳号, 库内推进值覆盖已发出最大号
     */
    @Test
    @Timeout(120)
    void takeUnderConcurrency() throws Exception {
        int threads = 32;
        int singleTimes = 20;
        int batchTimes = 2;
        int batchSize = 10;

        applyIfAbsent(TEST_BIZ_TAG_SERVICE);
        long deadlocksBefore = deadlockCount();

        List<Long> issued = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    for (int s = 0; s < singleTimes; s++) {
                        Long value = commonIdSegmentService.take(TEST_BIZ_GROUP, TEST_BIZ_TAG_SERVICE);
                        assertNotNull(value, "并发取号不应出现空值");
                        issued.add(value);
                    }
                    for (int b = 0; b < batchTimes; b++) {
                        issued.addAll(commonIdSegmentService.take(TEST_BIZ_GROUP, TEST_BIZ_TAG_SERVICE, batchSize));
                    }
                    return null;
                }));
            }
            ready.await();
            start.countDown();

            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }

            int expected = threads * (singleTimes + batchTimes * batchSize);
            assertEquals(expected, issued.size(), "发出的号总数应与预期一致");

            Set<Long> distinct = ConcurrentHashMap.newKeySet();
            distinct.addAll(issued);
            assertEquals(issued.size(), distinct.size(), "发出的号不应存在重复");

            List<Long> sorted = new ArrayList<>(issued);
            sorted.sort(Long::compareTo);
            for (int i = 1; i < sorted.size(); i++) {
                assertEquals(sorted.get(i - 1) + 1, sorted.get(i).longValue(), "发出的号应连续无跳号");
            }

            IdSegmentPo after = idSegmentRepository.get(TEST_BIZ_GROUP, TEST_BIZ_TAG_SERVICE);
            assertTrue(after.getCurrentMaxId() >= sorted.get(sorted.size() - 1), "库内推进值不应小于已发出的最大号");
            assertEquals(deadlocksBefore, deadlockCount(), "并发取号期间不应发生数据库死锁");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 层级约束: 业务组未登记时申请业务名应被拒绝, 业务名未登记时申请号段应被拒绝
     */
    @Test
    @Timeout(30)
    void applyRequiresRegisteredParent() {
        ApplyTagRequest tagRequest = new ApplyTagRequest();
        tagRequest.setBizGroup("groupNotExists");
        tagRequest.setBizTag("tagAny");
        assertThrows(RuntimeException.class, () -> idTagRepository.apply(tagRequest), "业务组未登记时申请业务名应被拒绝");

        applyIfAbsent(TEST_BIZ_TAG_REPOSITORY);
        ApplyIdSegmentRequest segmentRequest = new ApplyIdSegmentRequest();
        segmentRequest.setBizGroup(TEST_BIZ_GROUP);
        segmentRequest.setBizTag("tagNotExists");
        assertThrows(RuntimeException.class, () -> idSegmentRepository.apply(segmentRequest), "业务名未登记时申请号段应被拒绝");
    }

    /**
     * 确保验证专用号段存在, 按业务组、业务名、号段的申请顺序逐级登记, 已存在时幂等跳过
     */
    private void applyIfAbsent(String bizTag) {
        ApplyGroupRequest groupRequest = new ApplyGroupRequest();
        groupRequest.setBizGroup(TEST_BIZ_GROUP);
        groupRequest.setDescription("并发验证专用业务组");
        idGroupRepository.apply(groupRequest);

        ApplyTagRequest tagRequest = new ApplyTagRequest();
        tagRequest.setBizGroup(TEST_BIZ_GROUP);
        tagRequest.setBizTag(bizTag);
        tagRequest.setDescription("并发验证专用号段");
        idTagRepository.apply(tagRequest);

        ApplyIdSegmentRequest request = new ApplyIdSegmentRequest();
        request.setBizGroup(TEST_BIZ_GROUP);
        request.setBizTag(bizTag);
        request.setCurrentMaxId(10000L);
        request.setStep(1000L);
        request.setDescription("并发验证专用号段");
        idSegmentRepository.apply(request);
    }

    /**
     * 读取数据库引擎的累计死锁次数, 按当前数据库类型取对应引擎口径
     */
    private long deadlockCount() {
        if (null == databaseProductName) {
            databaseProductName = jdbcTemplate.execute(
                    (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
        }
        if ("MySQL".equals(databaseProductName)) {
            return jdbcTemplate.queryForObject(
                    "select SUM_ERROR_RAISED from performance_schema.events_errors_summary_global_by_error where ERROR_NAME = 'ER_LOCK_DEADLOCK'",
                    Long.class);
        }
        return jdbcTemplate.queryForObject(
                "select deadlocks from pg_stat_database where datname = current_database()",
                Long.class);
    }
}
