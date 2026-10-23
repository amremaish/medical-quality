create table organizations (
    id         bigint       not null auto_increment,
    name       varchar(255) not null,
    org_type   varchar(40)  not null,
    status     varchar(40)  not null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    deleted_at datetime(6)  null,
    primary key (id)
);

alter table users
    modify column username varchar(320) not null,
    add column email varchar(320) null after username,
    add column first_name varchar(128) null after email,
    add column last_name varchar(128) null after first_name,
    add column must_change_password bit(1) not null default 0 after is_super_admin,
    add column organization_id bigint null after department_id,
    add constraint users_email_key unique (email),
    add constraint users_organization_id_fk
        foreign key (organization_id) references organizations (id);
