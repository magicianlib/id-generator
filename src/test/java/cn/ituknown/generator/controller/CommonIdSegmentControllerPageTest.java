package cn.ituknown.generator.controller;

import cn.ituknown.generator.repository.IdGroupRepository;
import cn.ituknown.generator.repository.IdSegmentRepository;
import cn.ituknown.generator.repository.IdTagRepository;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.ApplyIdSegmentRequest;
import cn.ituknown.generator.request.ApplyTagRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommonIdSegmentControllerPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdGroupRepository idGroupRepository;

    @Autowired
    private IdTagRepository idTagRepository;

    @Autowired
    private IdSegmentRepository idSegmentRepository;

    @Test
    void pageEndpointsReturnPagedData() throws Exception {
        // 以随机前缀造数: 3 个业务组, 归属组下 3 个标签与 3 个号段
        String prefix = "ctrltest" + System.currentTimeMillis();
        String ownerGroup = prefix + "-owner";
        applyGroup(ownerGroup);
        applyGroup(prefix + "-second");
        applyGroup(prefix + "-third");
        for (String suffix : new String[]{"alpha", "beta", "gamma"}) {
            String bizTag = prefix + "-" + suffix;
            applyTag(ownerGroup, bizTag);
            applySegment(ownerGroup, bizTag);
        }

        // 业务组分页: 按前缀模糊命中 3 个组, 每页 2 条
        mockMvc.perform(post("/api/common/page-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"pageSize\":2,\"bizGroup\":\"" + prefix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pagination.total").value(3))
                .andExpect(jsonPath("$.data.pagination.pages").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2));

        // 标签分页: 按所属组精确加名称模糊命中 3 个标签
        mockMvc.perform(post("/api/common/page-tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"pageSize\":2,\"bizGroup\":\"" + ownerGroup + "\",\"bizTag\":\"" + prefix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pagination.total").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(2));

        // 号段分页: 按所属组精确命中 3 个号段
        mockMvc.perform(post("/api/common/page-segment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":2,\"pageSize\":2,\"bizGroup\":\"" + ownerGroup + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pagination.total").value(3))
                .andExpect(jsonPath("$.data.pagination.current").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(1));
    }

    private void applyGroup(String bizGroup) {
        ApplyGroupRequest request = new ApplyGroupRequest();
        request.setBizGroup(bizGroup);
        idGroupRepository.apply(request);
    }

    private void applyTag(String bizGroup, String bizTag) {
        ApplyTagRequest request = new ApplyTagRequest();
        request.setBizGroup(bizGroup);
        request.setBizTag(bizTag);
        idTagRepository.apply(request);
    }

    private void applySegment(String bizGroup, String bizTag) {
        ApplyIdSegmentRequest request = new ApplyIdSegmentRequest();
        request.setBizGroup(bizGroup);
        request.setBizTag(bizTag);
        idSegmentRepository.apply(request);
    }
}
