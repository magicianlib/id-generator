package cn.ituknown.generator.repository;

import cn.ituknown.generator.po.IdGroupPo;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.PageGroupRequest;
import cn.ituknown.generator.result.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdGroupRepositoryPageTest {

    @Autowired
    private IdGroupRepository idGroupRepository;

    @Test
    void pageFiltersByGroupNameAndReturnsPaginationMeta() {
        // 以随机前缀造数, 与库中已有业务组隔离
        String prefix = "pgtest" + System.currentTimeMillis();
        applyGroup(prefix + "-alpha");
        applyGroup(prefix + "-beta");
        applyGroup(prefix + "-gamma");

        PageGroupRequest request = new PageGroupRequest();
        request.setBizGroup(prefix);
        request.setCurrent(1);
        request.setPageSize(2);

        Page<IdGroupPo> page = idGroupRepository.page(request);

        assertEquals(3, page.getPagination().getTotal());
        assertEquals(2, page.getPagination().getPages());
        assertEquals(2, page.getList().size());
    }

    private void applyGroup(String bizGroup) {
        ApplyGroupRequest request = new ApplyGroupRequest();
        request.setBizGroup(bizGroup);
        idGroupRepository.apply(request);
    }
}
