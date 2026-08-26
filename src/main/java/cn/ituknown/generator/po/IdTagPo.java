package cn.ituknown.generator.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 业务标签, 以业务组与业务名联合唯一定位
 */
@Getter
@Setter
@TableName("id_tag")
public class IdTagPo implements Serializable {

    private static final long serialVersionUID = 5743129086652741902L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_group")
    private String bizGroup;

    @TableField("biz_tag")
    private String bizTag;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}