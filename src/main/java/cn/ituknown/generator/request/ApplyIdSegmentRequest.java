package cn.ituknown.generator.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
@ToString
public class ApplyIdSegmentRequest extends AbstractRequest {

    /**
     * 所属业务组名, 仅允许英文字母/数字/下划线/中划线
     */
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    private String bizGroup;

    /**
     * 业务名, 仅允许英文字母/数字/下划线/中划线
     */
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    private String bizTag;

    @Min(1)
    private Long currentMaxId;

    @Min(1)
    private Long step;

    /**
     * 缓存段数下限, 缺省时以库内默认值为准
     */
    @Min(1)
    @Max(1000)
    private Integer cacheMinLimit;

    /**
     * 缓存段数上限, 缺省时以库内默认值为准
     */
    @Min(1)
    @Max(1000)
    private Integer cacheMaxLimit;

    private String description;
}