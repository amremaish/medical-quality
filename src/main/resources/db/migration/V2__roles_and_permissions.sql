create table roles (
    id         bigint       not null auto_increment,
    name       varchar(100) not null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    deleted_at datetime(6)  null,
    primary key (id),
    constraint roles_name_key unique (name)
);

create table permissions (
    id         bigint       not null auto_increment,
    name       varchar(100) not null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    deleted_at datetime(6)  null,
    primary key (id),
    constraint permissions_name_key unique (name)
);

create table role_permissions (
    id            bigint      not null auto_increment,
    role_id       bigint      not null,
    permission_id bigint      not null,
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    deleted_at    datetime(6) null,
    primary key (id),
    constraint role_permissions_role_id_fk
        foreign key (role_id) references roles (id),
    constraint role_permissions_permission_id_fk
        foreign key (permission_id) references permissions (id),
    constraint role_permissions_role_permission_key unique (role_id, permission_id)
);

alter table users
    add column role_id bigint null after password_hash,
    add column is_super_admin bit(1) not null default 0 after enabled,
    add constraint users_role_id_fk foreign key (role_id) references roles (id);

alter table users
    drop column role;
