create table findings (
    id                 bigint      not null auto_increment,
    audit_id           bigint      null,
    department_id      bigint      null,
    gsr_code           varchar(20) not null,
    element_index      int         not null,
    description        text        not null,
    risk_level         varchar(20) not null,
    category           varchar(40) not null,
    status             varchar(20) not null,
    created_by_user_id bigint      null,
    created_at         datetime(6) not null,
    updated_at         datetime(6) not null,
    deleted_at         datetime(6) null,
    primary key (id),
    constraint findings_audit_id_fk
        foreign key (audit_id) references audits (id),
    constraint findings_department_id_fk
        foreign key (department_id) references departments (id),
    constraint findings_created_by_user_id_fk
        foreign key (created_by_user_id) references users (id)
);

create table capas (
    id                  bigint       not null auto_increment,
    finding_id          bigint       not null,
    corrective_action   text         not null,
    preventive_action   text         null,
    responsible_person  varchar(200) not null,
    target_date         date         null,
    progress            int          not null,
    status              varchar(20)  not null,
    created_by_user_id  bigint       null,
    created_at          datetime(6)  not null,
    updated_at          datetime(6)  not null,
    deleted_at          datetime(6)  null,
    primary key (id),
    constraint capas_finding_id_fk
        foreign key (finding_id) references findings (id),
    constraint capas_created_by_user_id_fk
        foreign key (created_by_user_id) references users (id)
);

create table documents (
    id                   bigint       not null auto_increment,
    category             varchar(40)  not null,
    original_name        varchar(255) not null,
    stored_name          varchar(255) not null,
    mime_type            varchar(120) null,
    size_bytes           bigint       not null,
    uploaded_by_user_id  bigint       null,
    created_at           datetime(6)  not null,
    updated_at           datetime(6)  not null,
    deleted_at           datetime(6)  null,
    primary key (id),
    constraint documents_uploaded_by_user_id_fk
        foreign key (uploaded_by_user_id) references users (id)
);

create table evidence (
    id            bigint      not null auto_increment,
    department_id bigint      null,
    audit_id      bigint      null,
    finding_id    bigint      null,
    document_id   bigint      not null,
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    deleted_at    datetime(6) null,
    primary key (id),
    constraint evidence_department_id_fk
        foreign key (department_id) references departments (id),
    constraint evidence_audit_id_fk
        foreign key (audit_id) references audits (id),
    constraint evidence_finding_id_fk
        foreign key (finding_id) references findings (id),
    constraint evidence_document_id_fk
        foreign key (document_id) references documents (id)
);
