create schema `id_generator_db` collate utf8mb4_bin;

create table `id_generator_db`.`id_segment`
(
    id             bigint auto_increment                     not null primary key comment '主键',
    biz_group      varchar(64)                               not null comment '业务组',
    biz_tag        varchar(64)                               not null comment '业务名',
    current_max_id bigint unsigned                           not null comment '当前已分配出去的最大 ID 值',
    step           bigint unsigned default 1000              not null comment '步阶',
    description    varchar(500)                              null comment '备注说明',
    created_at     datetime        default CURRENT_TIMESTAMP not null comment '创建时间(UTC)',
    updated_at     datetime        default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间(UTC)',
    unique key uk_biz_group_biz_tag (biz_group, biz_tag)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;