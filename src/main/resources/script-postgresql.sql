-- 库
create database id_generator_db;

-- 先进入库, 再执行后续 sql
-- connect id_generator_db;

--
-- 表 public.id_group
--
create table public.id_group
(
    id          bigserial
        constraint id_group_pk
            primary key,
    biz_group   text not null,
    description text,
    created_at  timestamptz default now(),
    updated_at  timestamptz default now()
);

comment on table public.id_group is '业务组';
comment on column public.id_group.id is '主键ID';
comment on column public.id_group.biz_group is '业务组';
comment on column public.id_group.description is '备注说明';
comment on column public.id_group.created_at is '创建时间';
comment on column public.id_group.updated_at is '更新时间';

create unique index id_group_biz_group_uindex
    on public.id_group (biz_group);

--
-- 表 public.id_group
--
create table public.id_tag
(
    id          bigserial
        constraint id_tag_pk
            primary key,
    biz_group   text not null,
    biz_tag     text not null,
    description text,
    created_at  timestamptz default now(),
    updated_at  timestamptz default now()
);

comment on table public.id_tag is '业务标签';
comment on column public.id_tag.id is '主键ID';
comment on column public.id_tag.biz_group is '业务组';
comment on column public.id_tag.biz_tag is '业务名';
comment on column public.id_tag.description is '备注说明';
comment on column public.id_tag.created_at is '创建时间';
comment on column public.id_tag.updated_at is '更新时间';

create unique index id_tag_biz_group__uindex
    on public.id_tag (biz_group, biz_tag);


--
-- 表 public.id_segment
--
create table public.id_segment
(
    id             bigserial
        constraint id_segment_pk
            primary key,
    biz_group      text                     not null,
    biz_tag        text                     not null,
    current_max_id bigint      default 0    not null,
    step           bigint      default 1000 not null,
    description    text,
    created_at     timestamptz default now(),
    updated_at     timestamptz default now()
);

comment on table public.id_segment is 'ID段';
comment on column public.id_segment.biz_group is '业务组';
comment on column public.id_segment.biz_tag is '业务名';
comment on column public.id_segment.current_max_id is '当前已分配出去的最大 ID 值';
comment on column public.id_segment.step is '步阶';
comment on column public.id_tag.description is '备注说明';
comment on column public.id_tag.created_at is '创建时间';
comment on column public.id_tag.updated_at is '更新时间';

create unique index id_segment_biz_group__uindex
    on public.id_segment (biz_group, biz_tag);