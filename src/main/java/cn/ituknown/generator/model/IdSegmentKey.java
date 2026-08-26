package cn.ituknown.generator.model;

import lombok.Value;

/**
 * 号段定位键, 以业务组与业务名唯一定位一个序列
 */
@Value
public class IdSegmentKey {

    /**
     * 业务组
     */
    String bizGroup;

    /**
     * 业务名
     */
    String bizTag;
}