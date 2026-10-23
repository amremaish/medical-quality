create table assessors (
    id         bigint       not null auto_increment,
    code       varchar(20)  not null,
    name_ar    varchar(200) not null,
    name_en    varchar(200) not null,
    note       varchar(300) null,
    active     bit(1)       not null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    deleted_at datetime(6)  null,
    primary key (id),
    constraint assessors_code_key unique (code)
);

create table departments (
    id             bigint       not null auto_increment,
    code           varchar(20)  not null,
    name_ar        varchar(200) not null,
    name_en        varchar(200) not null,
    dept_type      varchar(80)  not null,
    head_name      varchar(200) null,
    coordinator_name varchar(200) null,
    assessor_id    bigint       null,
    status         varchar(40)  not null,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    deleted_at     datetime(6)  null,
    primary key (id),
    constraint departments_code_key unique (code),
    constraint departments_assessor_id_fk
        foreign key (assessor_id) references assessors (id)
);

create table units (
    id            bigint       not null auto_increment,
    department_id bigint       not null,
    name          varchar(200) not null,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    deleted_at    datetime(6)  null,
    primary key (id),
    constraint units_department_id_fk
        foreign key (department_id) references departments (id)
);

alter table users
    add column assessor_id bigint null after is_super_admin,
    add column department_id bigint null after assessor_id,
    add constraint users_assessor_id_fk foreign key (assessor_id) references assessors (id),
    add constraint users_department_id_fk foreign key (department_id) references departments (id);

create table user_department_access (
    id            bigint      not null auto_increment,
    user_id       bigint      not null,
    department_id bigint      not null,
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    deleted_at    datetime(6) null,
    primary key (id),
    constraint user_department_access_user_department_key unique (user_id, department_id),
    constraint user_department_access_user_id_fk
        foreign key (user_id) references users (id),
    constraint user_department_access_department_id_fk
        foreign key (department_id) references departments (id)
);

alter table audits
    add column department_id bigint null after department_name,
    add constraint audits_department_id_fk foreign key (department_id) references departments (id);
