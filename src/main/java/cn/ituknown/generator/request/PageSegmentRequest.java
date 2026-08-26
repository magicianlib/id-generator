package cn.ituknown.generator.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PageSegmentRequest extends AbstractPageRequest {

    /**
     * 所属业务组名, 可选, 精确匹配
     */
    private String bizGroup;

    /**
     * 业务名, 可选, 精确匹配
     */
    private String bizTag;
}
