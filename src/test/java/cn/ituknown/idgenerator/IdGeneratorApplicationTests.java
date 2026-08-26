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
                .post("/api/idSegment/take")
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
                .post("/api/idSegment/take/{n}", 10)
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

}