package cn.ituknown.generator.repository;

import cn.ituknown.generator.po.IdTagPo;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.ApplyTagRequest;
import cn.ituknown.generator.request.PageTagRequest;
import cn.ituknown.generator.result.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdTagRepositoryPageTest {

    @Autowired
    private IdGroupRepository idGroupRepository;

    @Autowired
    private IdTagRepository idTagRepository;

    @Test
    void pageFiltersByGroupExactlyAndTagNameFuzzily() {
        // 以随机前缀造数, 与库中已有标签隔离
        String prefix = "tgtest" + System.currentTimeMillis();
        String ownerGroup = prefix + "-owner";
        String otherGroup = prefix + "-other";
        applyGroup(ownerGroup);
        applyGroup(otherGroup);

        // 归属业务组下造 3 个标签, 另一个业务组下放一个同前缀标签, 应被精确条件排除
        applyTag(ownerGroup, prefix + "-alpha");
        applyTag(ownerGroup, prefix + "-beta");
        applyTag(ownerGroup, prefix + "-gamma");
        applyTag(otherGroup, prefix + "-delta");

        PageTagRequest request = new PageTagRequest();
        request.setBizGroup(ownerGroup);
        request.setBizTag(prefix);
        request.setCurrent(1);
        request.setPageSize(2);

        Page<IdTagPo> page = idTagRepository.page(request);

        assertEquals(3, page.getPagination().getTotal());
        assertEquals(2, page.getPagination().getPages());
        assertEquals(2, page.getList().size());
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
}
