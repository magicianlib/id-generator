package cn.ituknown.idgenerator.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class ApplyIdSegmentRequest implements Serializable {

    @NotBlank
    private String bizGroup;

    @NotBlank
    private String bizTag;

    @Min(1)
    private Long currentMaxId;

    @Min(1)
    private Long step;

    private String description;
}