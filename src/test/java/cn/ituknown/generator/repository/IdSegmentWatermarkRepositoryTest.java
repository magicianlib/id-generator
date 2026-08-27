package cn.ituknown.generator.repository;

import cn.ituknown.generator.model.SegmentSupply;
import cn.ituknown.generator.po.IdSegmentPo;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.ApplyIdSegmentRequest;
import cn.ituknown.generator.request.ApplyTagRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 每标签缓存水位验证: 申请时登记的水位应落库, 并随每次领段供给回传
 */
@SpringBootTest
class IdSegmentWatermarkRepositoryTest {

    @Resource
    private IdGroupRepository idGroupRepository;

    @Resource
    private IdTagRepository idTagRepository;

    @Resource
    private IdSegmentRepository idSegmentRepository;

    @Test
    void applyCarriesWatermarkAndSupplyReturnsIt() {
        // 以随机前缀造数, 与库中已有号段隔离
        String prefix = "wmtest" + System.currentTimeMillis();
        String bizGroup = prefix + "-group";
        String bizTag = prefix + "-tag";
        applyGroup(bizGroup);
        applyTag(bizGroup, bizTag);

        // 登记时声明该标签的缓存水位为下限 1、上限 2
        ApplyIdSegmentRequest request = new ApplyIdSegmentRequest();
        request.setBizGroup(bizGroup);
        request.setBizTag(bizTag);
        request.setCacheMinLimit(1);
        request.setCacheMaxLimit(2);
        idSegmentRepository.apply(request);

        IdSegmentPo registered = idSegmentRepository.get(bizGroup, bizTag);
        assertEquals(1, registered.getCacheMinLimit().intValue(), "登记的下限应落库");
        assertEquals(2, registered.getCacheMaxLimit().intValue(), "登记的上限应落库");

        SegmentSupply supply = idSegmentRepository.nextSegmentRange(bizGroup, bizTag);
        assertNotNull(supply.getRange(), "领段应给出可发号区间");
        assertEquals(1, supply.getMinLimit().intValue(), "供给应携带该标签生效的下限");
        assertEquals(2, supply.getMaxLimit().intValue(), "供给应携带该标签生效的上限");
    }

    /**
     * 缓存水位下限大于上限时, 申请应被拒绝且不落库
     */
    @Test
    void applyRejectsMinLimitAboveMaxLimit() {
        String prefix = "wmtest" + System.currentTimeMillis();
        String bizGroup = prefix + "-group";
        String bizTag = prefix + "-tag";
        applyGroup(bizGroup);
        applyTag(bizGroup, bizTag);

        ApplyIdSegmentRequest request = new ApplyIdSegmentRequest();
        request.setBizGroup(bizGroup);
        request.setBizTag(bizTag);
        request.setCacheMinLimit(5);
        request.setCacheMaxLimit(2);

        assertThrows(RuntimeException.class, () -> idSegmentRepository.apply(request), "下限大于上限的缓存水位应被拒绝");
        assertNull(idSegmentRepository.get(bizGroup, bizTag), "被拒绝的申请不应落库");
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
