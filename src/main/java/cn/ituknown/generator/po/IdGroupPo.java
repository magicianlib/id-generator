package cn.ituknown.generator.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("id_group")
public class IdGroupPo implements Serializable {

    private static final long serialVersionUID = 6317084521940172853L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业务组名, 全局唯一
     */
    @TableField("biz_group")
    private String bizGroup;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}