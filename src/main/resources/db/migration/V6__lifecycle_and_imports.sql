alter table audits
    add column submitted_at datetime(6) null after created_by_user_id,
    add column submitted_by_user_id bigint null after submitted_at,
    add constraint audits_submitted_by_user_id_fk
        foreign key (submitted_by_user_id) references users (id);

create table audit_versions (
    id                 bigint      not null auto_increment,
    audit_id           bigint      not null,
    version_number     int         not null,
    changes_json       text        not null,
    changed_by_user_id bigint      null,
    created_at         datetime(6) not null,
    updated_at         datetime(6) not null,
    deleted_at         datetime(6) null,
    primary key (id),
    constraint audit_versions_audit_id_fk
        foreign key (audit_id) references audits (id)
);

create table audit_timeline (
    id         bigint       not null auto_increment,
    audit_id   bigint       not null,
    event_type varchar(40)  not null,
    detail     varchar(500) null,
    user_id    bigint       null,
    username   varchar(100) null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    deleted_at datetime(6)  null,
    primary key (id),
    constraint audit_timeline_audit_id_fk
        foreign key (audit_id) references audits (id)
);

create table audit_lifecycle_log (
    id            bigint       not null auto_increment,
    audit_id      bigint       null,
    department_id bigint       null,
    operation     varchar(40)  not null,
    reason        varchar(500) null,
    user_id       bigint       null,
    username      varchar(100) null,
    ip_address    varchar(80)  null,
    computer      varchar(80)  null,
    browser       varchar(80)  null,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    deleted_at    datetime(6)  null,
    primary key (id)
);

create table audit_imports (
    id                  bigint       not null auto_increment,
    original_name       varchar(255) not null,
    stored_name         varchar(255) not null,
    mime_type           varchar(120) null,
    size_bytes          bigint       not null,
    status              varchar(40)  not null,
    parse_error         varchar(500) null,
    extracted_json      longtext     null,
    audit_id            bigint       null,
    uploaded_by_user_id bigint       null,
    created_at          datetime(6)  not null,
    updated_at          datetime(6)  not null,
    deleted_at          datetime(6)  null,
    primary key (id),
    constraint audit_imports_audit_id_fk
        foreign key (audit_id) references audits (id),
    constraint audit_imports_uploaded_by_user_id_fk
        foreign key (uploaded_by_user_id) references users (id)
);
