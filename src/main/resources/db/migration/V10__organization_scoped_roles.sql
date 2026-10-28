DELIMITER $$
CREATE PROCEDURE mq_v10_assert(IN a_count INT, IN a_message VARCHAR(120))
BEGIN
    IF a_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = a_message;
    END IF;
END$$
DELIMITER ;

SET @c = (SELECT COUNT(*) FROM (SELECT name FROM roles GROUP BY name HAVING COUNT(*) > 1) d);
CALL mq_v10_assert(@c, 'V10 duplicate role names');

SET @c = (SELECT COUNT(*) FROM users WHERE is_super_admin = 0 AND organization_id IS NULL AND deleted_at IS NULL);
CALL mq_v10_assert(@c, 'V10 staff without a hospital');

SET @c = (SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id
    WHERE r.name NOT IN (
        'System Administrator', 'Hospital Owner', 'Quality Director', 'Quality Supervisor',
        'Assessor', 'Department Coordinator', 'View Only'));
CALL mq_v10_assert(@c, 'V10 unexpected role name');

SET @c = (SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id
    WHERE (u.is_super_admin = 1 AND r.name <> 'System Administrator')
       OR (u.is_super_admin = 0 AND r.name = 'System Administrator'));
CALL mq_v10_assert(@c, 'V10 system role assignment');

SET @c = (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND role_id IS NULL);
CALL mq_v10_assert(@c, 'V10 user without a role');

SET @c = (SELECT COUNT(*) FROM users u JOIN assessors a ON a.id = u.assessor_id
    WHERE u.organization_id IS NULL OR a.organization_id IS NULL OR u.organization_id <> a.organization_id);
CALL mq_v10_assert(@c, 'V10 assessor organization mismatch');

SET @c = (SELECT COUNT(*) FROM users u JOIN departments d ON d.id = u.department_id
    WHERE u.organization_id IS NULL OR d.organization_id IS NULL OR u.organization_id <> d.organization_id);
CALL mq_v10_assert(@c, 'V10 department organization mismatch');

SET @c = (SELECT COUNT(*) FROM user_department_access x
    JOIN users u ON u.id = x.user_id
    JOIN departments d ON d.id = x.department_id
    WHERE x.deleted_at IS NULL
      AND (u.organization_id IS NULL OR d.organization_id IS NULL OR u.organization_id <> d.organization_id));
CALL mq_v10_assert(@c, 'V10 department access organization mismatch');

SET @c = (SELECT COUNT(*) FROM audits a
    JOIN departments d ON d.id = a.department_id
    JOIN users u ON u.id = a.created_by_user_id
    WHERE a.deleted_at IS NULL AND u.organization_id IS NOT NULL AND u.organization_id <> d.organization_id);
CALL mq_v10_assert(@c, 'V10 audit organization mismatch');

SET @owners_before = (
    SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id
    WHERE r.name = 'Hospital Owner' AND u.enabled = 1 AND u.deleted_at IS NULL
);

alter table roles
    add column role_code varchar(40) null,
    add column organization_id bigint null,
    add column built_in bit(1) null,
    add column template bit(1) null,
    add column scope_mode varchar(20) null,
    add constraint roles_organization_id_fk foreign key (organization_id) references organizations (id);

update roles
set role_code = case name
        when 'System Administrator' then 'SYSTEM_ADMINISTRATOR'
        when 'Hospital Owner' then 'HOSPITAL_OWNER'
        when 'Quality Director' then 'QUALITY_DIRECTOR'
        when 'Quality Supervisor' then 'QUALITY_SUPERVISOR'
        when 'Assessor' then 'ASSESSOR'
        when 'Department Coordinator' then 'DEPARTMENT_COORDINATOR'
        when 'View Only' then 'VIEW_ONLY'
    end,
    built_in = 1,
    template = case when name = 'System Administrator' then 0 else 1 end,
    scope_mode = case name
        when 'Assessor' then 'ASSESSOR'
        when 'Department Coordinator' then 'DEPARTMENT'
        when 'View Only' then 'DEPARTMENT_LIST'
        else 'HOSPITAL'
    end
where deleted_at is null;

alter table roles
    modify role_code varchar(40) not null,
    modify built_in bit(1) not null,
    modify template bit(1) not null,
    modify scope_mode varchar(20) not null;

alter table roles drop index roles_name_key;

alter table roles
    add column scope_key varchar(80) as (
        concat(role_code, ':', if(organization_id is null, 'platform', organization_id))
    ) stored,
    add unique key roles_scope_key (scope_key);

insert into roles (name, role_code, organization_id, built_in, template, scope_mode, created_at, updated_at)
select src.name, src.role_code, src.organization_id, 1, 0, src.scope_mode, current_timestamp(6), current_timestamp(6)
from (
    select t.name, t.role_code, o.id as organization_id, t.scope_mode
    from roles t
    join organizations o on o.deleted_at is null
    where t.template = 1 and t.deleted_at is null
) src;

insert into role_permissions (role_id, permission_id, created_at, updated_at)
select c.id, p.permission_id, current_timestamp(6), current_timestamp(6)
from roles c
join roles t on t.role_code = c.role_code and t.template = 1 and t.deleted_at is null
join role_permissions p on p.role_id = t.id and p.deleted_at is null
where c.organization_id is not null and c.built_in = 1 and c.template = 0 and c.deleted_at is null;

update users u
join roles current_role on current_role.id = u.role_id
join roles copy_role on copy_role.role_code = current_role.role_code
    and copy_role.organization_id = u.organization_id
    and copy_role.template = 0
    and copy_role.deleted_at is null
set u.role_id = copy_role.id
where u.is_super_admin = 0;

alter table user_department_access
    add column organization_id bigint null;

update user_department_access x
join users u on u.id = x.user_id
set x.organization_id = u.organization_id;

alter table user_department_access
    modify organization_id bigint not null;

alter table roles add unique key roles_id_org (id, organization_id);
alter table users add unique key users_id_org (id, organization_id);
alter table assessors add unique key assessors_id_org (id, organization_id);
alter table departments add unique key departments_id_org (id, organization_id);

alter table users
    add constraint users_role_org_fk foreign key (role_id, organization_id) references roles (id, organization_id),
    add constraint users_assessor_org_fk foreign key (assessor_id, organization_id) references assessors (id, organization_id),
    add constraint users_department_org_fk foreign key (department_id, organization_id) references departments (id, organization_id),
    add unique key users_assessor_id_key (assessor_id),
    add constraint users_org_required_chk check (is_super_admin = 1 or organization_id is not null),
    add constraint users_assessor_org_chk check (assessor_id is null or organization_id is not null),
    add constraint users_department_org_chk check (department_id is null or organization_id is not null);

alter table user_department_access
    add constraint user_department_access_user_org_fk
        foreign key (user_id, organization_id) references users (id, organization_id),
    add constraint user_department_access_department_org_fk
        foreign key (department_id, organization_id) references departments (id, organization_id);

create table password_setup_tokens (
    id          bigint       not null auto_increment,
    user_id     bigint       not null,
    token_hash  char(64)     not null,
    expires_at  datetime(6)  not null,
    used_at     datetime(6)  null,
    created_at  datetime(6)  not null,
    primary key (id),
    unique key password_setup_tokens_hash (token_hash),
    constraint password_setup_tokens_user_fk foreign key (user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

SET @c = (SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id
    WHERE u.is_super_admin = 0
      AND (r.organization_id IS NULL OR r.organization_id <> u.organization_id OR r.template = 1));
CALL mq_v10_assert(@c, 'V10 user still on a platform role');

SET @c = (SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id
    WHERE u.is_super_admin = 0 AND r.role_code = 'SYSTEM_ADMINISTRATOR');
CALL mq_v10_assert(@c, 'V10 staff on system role');

SET @c = (SELECT COUNT(*) FROM (
    SELECT o.id FROM organizations o
    LEFT JOIN roles r ON r.organization_id = o.id AND r.built_in = 1 AND r.template = 0 AND r.deleted_at IS NULL
    WHERE o.deleted_at IS NULL
    GROUP BY o.id
    HAVING COUNT(r.id) <> 6
) missing);
CALL mq_v10_assert(@c, 'V10 hospital is missing built-in roles');

SET @c = (SELECT COUNT(*) FROM roles c
    JOIN roles t ON t.role_code = c.role_code AND t.template = 1
    WHERE c.organization_id IS NOT NULL AND c.built_in = 1 AND c.deleted_at IS NULL
      AND (SELECT COUNT(*) FROM role_permissions p WHERE p.role_id = c.id AND p.deleted_at IS NULL)
       <> (SELECT COUNT(*) FROM role_permissions p WHERE p.role_id = t.id AND p.deleted_at IS NULL));
CALL mq_v10_assert(@c, 'V10 copied permissions do not match');

SET @c = (SELECT COUNT(*) FROM user_department_access x
    JOIN users u ON u.id = x.user_id
    JOIN departments d ON d.id = x.department_id
    WHERE x.organization_id <> u.organization_id OR x.organization_id <> d.organization_id);
CALL mq_v10_assert(@c, 'V10 department access was not filled');

SET @owners_after = (
    SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id
    WHERE r.role_code = 'HOSPITAL_OWNER' AND r.template = 0 AND u.enabled = 1 AND u.deleted_at IS NULL
);
CALL mq_v10_assert(IF(@owners_before = @owners_after, 0, 1), 'V10 owner count changed');

DROP PROCEDURE mq_v10_assert;
