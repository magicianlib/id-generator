package cn.ituknown.idgenerator;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdGeneratorApplicationTests {

    /**
     * 测试用号段维度, 与服务层测试保持一致
     */
    private static final String TEST_BIZ_GROUP = "commons";

    private static final String TEST_BIZ_TAG = "default";

    @Autowired
    private MockMvc mockMvc;

    private static final Logger LOGGER = LoggerFactory.getLogger(IdGeneratorApplicationTests.class);

    @Test
    void seqtake() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post("/api/common/takeSegment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bizGroup\":\"" + TEST_BIZ_GROUP + "\",\"bizTag\":\"" + TEST_BIZ_TAG + "\"}");

        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result -> {
                    String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    LOGGER.info("response: {}", response);
                });
    }

    @Test
    void seqtakeN() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post("/api/common/takeSegment/{n}", 10)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bizGroup\":\"" + TEST_BIZ_GROUP + "\",\"bizTag\":\"" + TEST_BIZ_TAG + "\"}");

        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result -> {
                    String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    LOGGER.info("response: {}", response);
                });
    }

    /**
     * 业务组与业务名仅允许英文字母/数字/下划线/中划线, 带其他字符的请求应被校验拒绝并返回失败业务码
     */
    @Test
    void seqtakeWithIllegalName() throws Exception {

        for (String illegal : new String[]{"订单", "main tag", "order.1", "tag!"}) {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                    .post("/api/common/takeSegment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"bizGroup\":\"" + illegal + "\",\"bizTag\":\"" + illegal + "\"}");

            mockMvc.perform(request)
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1));
        }

        // 下划线与中划线属于合法命名, 应通过校验进入业务处理
        MockHttpServletRequestBuilder legal = MockMvcRequestBuilders
                .post("/api/common/takeSegment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bizGroup\":\"order-1\",\"bizTag\":\"main_tag\"}");

        mockMvc.perform(legal)
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * 业务名未登记时申请号段应被拒绝, 异常经全局兜底统一转失败响应体
     */
    @Test
    void applySegmentWithUnregisteredTag() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post("/api/common/applySegment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bizGroup\":\"" + TEST_BIZ_GROUP + "\",\"bizTag\":\"tagNotExists\"}");

        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1))
                .andDo(result -> {
                    String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    LOGGER.info("response: {}", response);
                });
    }

}