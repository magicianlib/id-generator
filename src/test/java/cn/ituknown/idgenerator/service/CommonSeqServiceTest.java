package cn.ituknown.idgenerator.service;

import cn.ituknown.idgenerator.model.IdSegmentKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class CommonSeqServiceTest {

    /**
     * 测试用号段维度, 需提前通过申请接口或建表语句写入该号段
     */
    private static final String TEST_BIZ_GROUP = "commons";

    private static final String TEST_BIZ_TAG = "default";

    @Autowired
    private CommonIdSegmentService commonSeqService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSeqServiceTest.class);

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void exist() {
    }

    @Test
    void take() {
        LOGGER.info("==== test take");
        Long data = commonSeqService.take(TEST_BIZ_GROUP, TEST_BIZ_TAG);
        LOGGER.info("==== test take: {}", data);
    }

    @Test
    void takeN() {
        LOGGER.info("==== test takeN");
        List<Long> data = commonSeqService.take(TEST_BIZ_GROUP, TEST_BIZ_TAG, 10);
        LOGGER.info("==== test takeN: {}", data);
    }

    @Test
    void segmentList() {
        LOGGER.info("==== test segmentList");
        List<IdSegmentKey> data = commonSeqService.segmentList();
        LOGGER.info("==== test segmentList: {}", data);
    }
}