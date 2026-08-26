create schema `id_generator_db` collate utf8mb4_bin;

create table `id_generator_db`.`id_group`
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    biz_group   varchar(100)                       not null comment '业务组',
    description varchar(500)                       null comment '备注说明',
    created_at  datetime default CURRENT_TIMESTAMP null comment '创建时间(UTC)',
    updated_at  datetime default CURRENT_TIMESTAMP null comment '更新时间(UTC)',
    constraint uq_biz_group
        unique (biz_group)
)
    comment '业务组' charset = utf8mb4;

create table `id_generator_db`.`id_tag`
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    biz_group   varchar(100)                       not null comment '业务组',
    biz_tag     varchar(100)                       not null comment '业务名',
    description varchar(500)                       null comment '备注说明',
    created_at  datetime default CURRENT_TIMESTAMP null comment '创建时间(UTC)',
    updated_at  datetime default CURRENT_TIMESTAMP null comment '更新时间(UTC)',
    constraint uq_biz
        unique (biz_group, biz_tag)
)
    comment '业务标签' charset = utf8mb4;


create table `id_generator_db`.`id_segment`
(
    id             bigint auto_increment comment '主键'
        primary key,
    biz_group      varchar(100)                              not null comment '业务组',
    biz_tag        varchar(100)                              not null comment '业务名',
    current_max_id bigint unsigned                           not null comment '当前已分配出去的最大 ID 值',
    step           bigint unsigned default '1000'            not null comment '步阶',
    description    varchar(500)                              null comment '备注说明',
    created_at     datetime        default CURRENT_TIMESTAMP not null comment '创建时间(UTC)',
    updated_at     datetime        default CURRENT_TIMESTAMP null comment '更新时间(UTC)',
    constraint uk_biz_group_biz_tag
        unique (biz_group, biz_tag)
)
    comment 'ID段' charset = utf8mb4;