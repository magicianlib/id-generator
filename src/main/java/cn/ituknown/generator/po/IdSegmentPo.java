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
@TableName("id_segment")
public class IdSegmentPo implements Serializable {

    private static final long serialVersionUID = 5320388649480031296L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_group")
    private String bizGroup;

    @TableField("biz_tag")
    private String bizTag;

    @TableField("current_max_id")
    private Long currentMaxId;

    @TableField("step")
    private Long step;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}