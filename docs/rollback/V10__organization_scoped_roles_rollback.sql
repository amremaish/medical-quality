-- Manual rollback for V10. Do not place this file in db/migration.
-- Safe only after V10 succeeded and before the new build has created custom roles,
-- setup tokens, or invited users. Otherwise restore the pre-migration backup.
-- Restoring that backup discards every write committed after it was taken.

update users u
join roles current_role on current_role.id = u.role_id
join roles template_role on template_role.role_code = current_role.role_code
    and template_role.template = 1
    and template_role.organization_id is null
set u.role_id = template_role.id
where u.is_super_admin = 0;

alter table user_department_access drop foreign key user_department_access_user_org_fk;
alter table user_department_access drop foreign key user_department_access_department_org_fk;
alter table users drop foreign key users_role_org_fk;
alter table users drop foreign key users_assessor_org_fk;
alter table users drop foreign key users_department_org_fk;
alter table users drop check users_org_required_chk;
alter table users drop check users_assessor_org_chk;
alter table users drop check users_department_org_chk;
alter table users drop index users_assessor_id_key;
alter table users drop index users_id_org;
alter table roles drop index roles_id_org;
alter table assessors drop index assessors_id_org;
alter table departments drop index departments_id_org;

delete p from role_permissions p
join roles r on r.id = p.role_id
where r.organization_id is not null;

delete from roles where organization_id is not null;

drop table password_setup_tokens;

alter table user_department_access drop column organization_id;
alter table roles drop index roles_scope_key;
alter table roles drop column scope_key;
alter table roles drop foreign key roles_organization_id_fk;
alter table roles
    drop column role_code,
    drop column organization_id,
    drop column built_in,
    drop column template,
    drop column scope_mode;
alter table roles add unique key roles_name_key (name);
