-- Manual rollback for V11. Do not place this file in db/migration.
-- Run only when restoring this schema by hand. A failed V11 stops before later
-- migrations; Flyway records it as failed until the database is repaired and
-- the migration can run again from the start. Prefer restoring the pre-migration
-- backup when any later write must be kept consistent with the old schema.

drop table if exists user_permission_override_events;
drop table if exists user_permission_overrides;

delete rp from role_permissions rp
join permissions p on p.id = rp.permission_id
where p.name = 'MANAGE_USER_PERMISSION_OVERRIDES';

delete from permissions where name = 'MANAGE_USER_PERMISSION_OVERRIDES';
