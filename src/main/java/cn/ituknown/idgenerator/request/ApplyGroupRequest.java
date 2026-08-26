package cn.ituknown.idgenerator.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class ApplyGroupRequest implements Serializable {

    private static final long serialVersionUID = 4821073645290875316L;

    /**
     * 业务组名, 仅允许英文字母/数字/下划线/中划线
     */
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    private String bizGroup;

    private String description;
}