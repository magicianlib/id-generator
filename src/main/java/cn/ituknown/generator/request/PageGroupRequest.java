package cn.ituknown.generator.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PageGroupRequest extends AbstractPageRequest {

    /**
     * 业务组名, 可选, 模糊匹配
     */
    private String bizGroup;
}
