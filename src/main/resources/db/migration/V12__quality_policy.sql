insert into permissions (name, created_at, updated_at)
select 'QUALITY_POLICY_VIEW', current_timestamp(6), current_timestamp(6)
where not exists (select 1 from permissions where name = 'QUALITY_POLICY_VIEW');

insert into permissions (name, created_at, updated_at)
select 'QUALITY_POLICY_EDIT', current_timestamp(6), current_timestamp(6)
where not exists (select 1 from permissions where name = 'QUALITY_POLICY_EDIT');

insert into role_permissions (role_id, permission_id, created_at, updated_at)
select r.id, p.id, current_timestamp(6), current_timestamp(6)
from roles r
join permissions p on p.name = 'QUALITY_POLICY_VIEW'
where r.deleted_at is null
  and r.role_code in ('SYSTEM_ADMINISTRATOR', 'QUALITY_DIRECTOR', 'HOSPITAL_OWNER', 'QUALITY_SUPERVISOR')
  and not exists (
      select 1 from role_permissions existing
      where existing.role_id = r.id and existing.permission_id = p.id
  );

insert into role_permissions (role_id, permission_id, created_at, updated_at)
select r.id, p.id, current_timestamp(6), current_timestamp(6)
from roles r
join permissions p on p.name = 'QUALITY_POLICY_EDIT'
where r.deleted_at is null
  and r.role_code in ('SYSTEM_ADMINISTRATOR', 'QUALITY_DIRECTOR', 'HOSPITAL_OWNER')
  and not exists (
      select 1 from role_permissions existing
      where existing.role_id = r.id and existing.permission_id = p.id
  );

create table quality_policy_templates (
    id                  bigint       not null auto_increment,
    kind                varchar(20)  not null,
    name                varchar(200) not null,
    document_json       json         not null,
    created_by_user_id  bigint       null,
    created_at          datetime(6)  not null,
    updated_at          datetime(6)  not null,
    deleted_at          datetime(6)  null,
    primary key (id),
    key quality_policy_templates_kind (kind, deleted_at),
    constraint quality_policy_templates_kind_chk check (kind in ('SYSTEM', 'GROUP')),
    constraint quality_policy_templates_created_by_fk
        foreign key (created_by_user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

insert into quality_policy_templates (kind, name, document_json, created_at, updated_at)
select 'SYSTEM', 'System default', '{}', current_timestamp(6), current_timestamp(6)
where not exists (
    select 1 from quality_policy_templates where kind = 'SYSTEM' and deleted_at is null
);

create table quality_policy_versions (
    id                   bigint        not null auto_increment,
    organization_id      bigint        not null,
    version_number       int           not null,
    status               varchar(30)   not null,
    effective_from       date          not null,
    effective_until      date          null,
    document_json        json          not null,
    overrides_json       json          not null,
    reason               varchar(1000) not null,
    created_by_user_id   bigint        not null,
    approved_by_user_id  bigint        null,
    approved_at          datetime(6)   null,
    created_at           datetime(6)   not null,
    updated_at           datetime(6)   not null,
    deleted_at           datetime(6)   null,
    primary key (id),
    unique key quality_policy_versions_org_number (organization_id, version_number),
    key quality_policy_versions_window (organization_id, effective_from),
    constraint quality_policy_versions_status_chk check (
        status in ('PENDING_APPROVAL', 'SCHEDULED', 'ACTIVE', 'SUPERSEDED', 'REJECTED')
    ),
    constraint quality_policy_versions_org_fk
        foreign key (organization_id) references organizations (id),
    constraint quality_policy_versions_created_by_fk
        foreign key (created_by_user_id) references users (id),
    constraint quality_policy_versions_approved_by_fk
        foreign key (approved_by_user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table quality_policy_changes (
    id           bigint       not null auto_increment,
    version_id   bigint       not null,
    setting_key  varchar(160) not null,
    old_value    json         null,
    new_value    json         null,
    created_at   datetime(6)  not null,
    updated_at   datetime(6)  not null,
    deleted_at   datetime(6)  null,
    primary key (id),
    key quality_policy_changes_version (version_id, id),
    constraint quality_policy_changes_version_fk
        foreign key (version_id) references quality_policy_versions (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table quality_policy_group_members (
    id               bigint      not null auto_increment,
    organization_id  bigint      not null,
    template_id      bigint      not null,
    created_at       datetime(6) not null,
    updated_at       datetime(6) not null,
    deleted_at       datetime(6) null,
    active_organization_id bigint as (
        case when deleted_at is null then organization_id else null end
    ) stored,
    primary key (id),
    unique key quality_policy_group_members_active_org (active_organization_id),
    constraint quality_policy_group_members_org_fk
        foreign key (organization_id) references organizations (id),
    constraint quality_policy_group_members_template_fk
        foreign key (template_id) references quality_policy_templates (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table quality_policy_violations (
    id               bigint       not null auto_increment,
    organization_id  bigint       not null,
    version_id       bigint       not null,
    entity_type      varchar(40)  not null,
    entity_id        bigint       not null,
    setting_key      varchar(160) not null,
    detail           varchar(500) not null,
    resolved_at      datetime(6)  null,
    created_at       datetime(6)  not null,
    updated_at       datetime(6)  not null,
    deleted_at       datetime(6)  null,
    primary key (id),
    key quality_policy_violations_org (organization_id, resolved_at),
    constraint quality_policy_violations_org_fk
        foreign key (organization_id) references organizations (id),
    constraint quality_policy_violations_version_fk
        foreign key (version_id) references quality_policy_versions (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table audit_assessors (
    id              bigint      not null auto_increment,
    audit_id        bigint      not null,
    assessor_id     bigint      not null,
    lead_assessor   bit(1)      not null default 0,
    coi_declared    bit(1)      not null default 0,
    created_at      datetime(6) not null,
    updated_at      datetime(6) not null,
    deleted_at      datetime(6) null,
    active_pair     varchar(80) as (
        case when deleted_at is null then concat(audit_id, ':', assessor_id) else null end
    ) stored,
    primary key (id),
    unique key audit_assessors_active_pair (active_pair),
    key audit_assessors_audit (audit_id),
    key audit_assessors_assessor (assessor_id),
    constraint audit_assessors_audit_fk
        foreign key (audit_id) references audits (id),
    constraint audit_assessors_assessor_fk
        foreign key (assessor_id) references assessors (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

alter table assessors
    add column external_assessor bit(1) not null default 0;

alter table findings
    add column canonical_severity varchar(40) null,
    add column escalated bit(1) not null default 0,
    add column policy_version_id bigint null,
    add constraint findings_policy_version_fk
        foreign key (policy_version_id) references quality_policy_versions (id);

alter table capas
    add column policy_version_id bigint null,
    add column deadline_days int null,
    add column root_cause text null,
    add column extension_count int not null default 0,
    add column extension_reason varchar(500) null,
    add column verified_at datetime(6) null,
    add column verified_by_user_id bigint null,
    add constraint capas_policy_version_fk
        foreign key (policy_version_id) references quality_policy_versions (id),
    add constraint capas_verified_by_fk
        foreign key (verified_by_user_id) references users (id);

alter table audits
    add column policy_version_id bigint null,
    add column locked_at datetime(6) null,
    add constraint audits_policy_version_fk
        foreign key (policy_version_id) references quality_policy_versions (id);
