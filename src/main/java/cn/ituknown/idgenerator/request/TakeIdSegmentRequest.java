package cn.ituknown.idgenerator.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class TakeIdSegmentRequest implements Serializable {

    @NotBlank
    private String bizGroup;

    @NotBlank
    private String bizTag;
}