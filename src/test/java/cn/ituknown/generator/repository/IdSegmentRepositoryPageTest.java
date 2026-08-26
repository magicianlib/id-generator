package cn.ituknown.generator.repository;

import cn.ituknown.generator.po.IdSegmentPo;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.ApplyIdSegmentRequest;
import cn.ituknown.generator.request.ApplyTagRequest;
import cn.ituknown.generator.request.PageSegmentRequest;
import cn.ituknown.generator.result.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdSegmentRepositoryPageTest {

    @Autowired
    private IdGroupRepository idGroupRepository;

    @Autowired
    private IdTagRepository idTagRepository;

    @Autowired
    private IdSegmentRepository idSegmentRepository;

    @Test
    void pageFiltersByGroupAndTagExactly() {
        // 以随机前缀造数, 与库中已有号段隔离
        String prefix = "sgtest" + System.currentTimeMillis();
        String ownerGroup = prefix + "-owner";
        String otherGroup = prefix + "-other";
        applyGroup(ownerGroup);
        applyGroup(otherGroup);

        // 归属业务组下造 3 个号段, 另一个业务组下放一个同业务名号段, 应被精确条件排除
        applySegment(ownerGroup, prefix + "-alpha");
        applySegment(ownerGroup, prefix + "-beta");
        applySegment(ownerGroup, prefix + "-gamma");
        applySegment(otherGroup, prefix + "-alpha");

        // 只按业务组过滤, 验证排除他组号段并返回分页元数据
        PageSegmentRequest groupOnly = new PageSegmentRequest();
        groupOnly.setBizGroup(ownerGroup);
        groupOnly.setCurrent(1);
        groupOnly.setPageSize(2);

        Page<IdSegmentPo> groupPage = idSegmentRepository.page(groupOnly);

        assertEquals(3, groupPage.getPagination().getTotal());
        assertEquals(2, groupPage.getPagination().getPages());
        assertEquals(2, groupPage.getList().size());

        // 业务组加业务名交集过滤, 只命中归属组下的那一个号段
        PageSegmentRequest groupAndTag = new PageSegmentRequest();
        groupAndTag.setBizGroup(ownerGroup);
        groupAndTag.setBizTag(prefix + "-alpha");
        groupAndTag.setCurrent(1);
        groupAndTag.setPageSize(2);

        Page<IdSegmentPo> onePage = idSegmentRepository.page(groupAndTag);

        assertEquals(1, onePage.getPagination().getTotal());
        assertEquals(1, onePage.getList().size());
        assertEquals(ownerGroup, onePage.getList().get(0).getBizGroup());
        assertEquals(prefix + "-alpha", onePage.getList().get(0).getBizTag());
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
        applyTag(bizGroup, bizTag);

        ApplyIdSegmentRequest request = new ApplyIdSegmentRequest();
        request.setBizGroup(bizGroup);
        request.setBizTag(bizTag);
        idSegmentRepository.apply(request);
    }
}
