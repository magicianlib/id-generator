package cn.ituknown.generator.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class ApplyIdSegmentRequest implements Serializable {

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

    private String description;
}