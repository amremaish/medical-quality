create table gsr_chapters (
    id         bigint       not null auto_increment,
    name       varchar(120) not null,
    sort_order int          not null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    deleted_at datetime(6)  null,
    primary key (id),
    constraint gsr_chapters_name_key unique (name)
);

create table gsr_standards (
    id            bigint       not null auto_increment,
    chapter_id    bigint       not null,
    code          varchar(20)  not null,
    title         varchar(200) not null,
    handbook_ref  varchar(20)  not null,
    intent        varchar(500) not null,
    sort_order    int          not null,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    deleted_at    datetime(6)  null,
    primary key (id),
    constraint gsr_standards_code_key unique (code),
    constraint gsr_standards_chapter_id_fk
        foreign key (chapter_id) references gsr_chapters (id)
);

create table gsr_elements (
    id          bigint      not null auto_increment,
    standard_id bigint      not null,
    sort_order  int         not null,
    body        text        not null,
    created_at  datetime(6) not null,
    updated_at  datetime(6) not null,
    deleted_at  datetime(6) null,
    primary key (id),
    constraint gsr_elements_standard_id_fk
        foreign key (standard_id) references gsr_standards (id)
);

create table audits (
    id                 bigint       not null auto_increment,
    department_name    varchar(200) not null,
    auditor_name       varchar(200) not null,
    audit_date         date         not null,
    status             varchar(40)  not null,
    archived           bit(1)       not null,
    created_by_user_id bigint       null,
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    deleted_at         datetime(6)  null,
    primary key (id),
    constraint audits_created_by_user_id_fk
        foreign key (created_by_user_id) references users (id)
);

create table audit_scores (
    id          bigint      not null auto_increment,
    audit_id    bigint      not null,
    element_id  bigint      not null,
    score_value varchar(20) not null,
    created_at  datetime(6) not null,
    updated_at  datetime(6) not null,
    deleted_at  datetime(6) null,
    primary key (id),
    constraint audit_scores_audit_element_key unique (audit_id, element_id),
    constraint audit_scores_audit_id_fk
        foreign key (audit_id) references audits (id),
    constraint audit_scores_element_id_fk
        foreign key (element_id) references gsr_elements (id)
);

create table audit_comments (
    id         bigint      not null auto_increment,
    audit_id   bigint      not null,
    element_id bigint      not null,
    body       text        not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,
    primary key (id),
    constraint audit_comments_audit_element_key unique (audit_id, element_id),
    constraint audit_comments_audit_id_fk
        foreign key (audit_id) references audits (id),
    constraint audit_comments_element_id_fk
        foreign key (element_id) references gsr_elements (id)
);

create table mr_sections (
    id         bigint       not null auto_increment,
    name       varchar(160) not null,
    sort_order int          not null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    deleted_at datetime(6)  null,
    primary key (id),
    constraint mr_sections_name_key unique (name)
);

create table mr_items (
    id            bigint      not null auto_increment,
    section_id    bigint      not null,
    standard_code varchar(20) not null,
    eoc_no        varchar(10) not null,
    body          text        not null,
    sort_order    int         not null,
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    deleted_at    datetime(6) null,
    primary key (id),
    constraint mr_items_section_id_fk
        foreign key (section_id) references mr_sections (id)
);

create table mr_reviews (
    id                 bigint       not null auto_increment,
    hospital_name      varchar(200) not null,
    reviewer_name      varchar(200) not null,
    review_date        date         not null,
    department_name    varchar(200) not null,
    created_by_user_id bigint       null,
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    deleted_at         datetime(6)  null,
    primary key (id),
    constraint mr_reviews_created_by_user_id_fk
        foreign key (created_by_user_id) references users (id)
);

create table mr_records (
    id                 bigint      not null auto_increment,
    review_id          bigint      not null,
    code               varchar(10) not null,
    patient_identifier varchar(120) null,
    created_at         datetime(6) not null,
    updated_at         datetime(6) not null,
    deleted_at         datetime(6) null,
    primary key (id),
    constraint mr_records_review_code_key unique (review_id, code),
    constraint mr_records_review_id_fk
        foreign key (review_id) references mr_reviews (id)
);

create table mr_answers (
    id         bigint      not null auto_increment,
    record_id  bigint      not null,
    item_id    bigint      not null,
    legible    bit(1)      not null,
    accurate   bit(1)      not null,
    complete   bit(1)      not null,
    na         bit(1)      not null,
    comment    text        null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,
    primary key (id),
    constraint mr_answers_record_item_key unique (record_id, item_id),
    constraint mr_answers_record_id_fk
        foreign key (record_id) references mr_records (id),
    constraint mr_answers_item_id_fk
        foreign key (item_id) references mr_items (id)
);
