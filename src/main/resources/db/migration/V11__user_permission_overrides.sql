insert into permissions (name, created_at, updated_at)
select 'MANAGE_USER_PERMISSION_OVERRIDES', current_timestamp(6), current_timestamp(6)
where not exists (
    select 1 from permissions where name = 'MANAGE_USER_PERMISSION_OVERRIDES'
);

create table user_permission_overrides (
    id                   bigint       not null auto_increment,
    organization_id      bigint       not null,
    user_id              bigint       not null,
    permission_id        bigint       not null,
    effect               varchar(10)  not null,
    created_by_user_id   bigint       not null,
    updated_by_user_id   bigint       not null,
    created_at           datetime(6)  not null,
    updated_at           datetime(6)  not null,
    deleted_at           datetime(6)  null,
    active_pair          varchar(80) as (
        case when deleted_at is null then concat(user_id, ':', permission_id) else null end
    ) stored,
    primary key (id),
    unique key user_permission_overrides_active_pair (active_pair),
    key user_permission_overrides_user (user_id, deleted_at),
    constraint user_permission_overrides_effect_chk check (effect in ('ALLOW', 'DENY')),
    constraint user_permission_overrides_user_org_fk
        foreign key (user_id, organization_id) references users (id, organization_id),
    constraint user_permission_overrides_permission_fk
        foreign key (permission_id) references permissions (id),
    constraint user_permission_overrides_created_by_fk
        foreign key (created_by_user_id) references users (id),
    constraint user_permission_overrides_updated_by_fk
        foreign key (updated_by_user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table user_permission_override_events (
    id               bigint       not null auto_increment,
    organization_id  bigint       not null,
    user_id          bigint       not null,
    permission_id    bigint       not null,
    previous_effect  varchar(10)  null,
    new_effect       varchar(10)  null,
    actor_user_id    bigint       not null,
    created_at       datetime(6)  not null,
    primary key (id),
    key user_permission_override_events_user (user_id, created_at),
    constraint user_permission_override_events_effect_chk check (
        (previous_effect is null or previous_effect in ('ALLOW', 'DENY'))
        and (new_effect is null or new_effect in ('ALLOW', 'DENY'))
    ),
    constraint user_permission_override_events_user_org_fk
        foreign key (user_id, organization_id) references users (id, organization_id),
    constraint user_permission_override_events_permission_fk
        foreign key (permission_id) references permissions (id),
    constraint user_permission_override_events_actor_fk
        foreign key (actor_user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
