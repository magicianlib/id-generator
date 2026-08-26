package cn.ituknown.generator.request;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;

/**
 * 分页请求参数
 */
@Getter
@Setter
public abstract class AbstractPageRequest extends AbstractRequest {
    /**
     * 当前页码（从 1 开始）
     */
    @Min(1)
    private int current = 1;

    /**
     * 每页数量
     */
    @Min(1)
    private int pageSize = 10;
}