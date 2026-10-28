# Organization-scoped users and roles

Status: **Approved for implementation.**

This file is the design to implement. It keeps the earlier decisions on the directory gate, separate role routes, `role_code`, and organization foreign keys. This pass corrects the custom-role allowlist, the scope holes those permissions would inherit, invitation consumption, the last-owner lock, and migration retry. No application code, migration, configuration, or test was changed while writing it.

Checked against the code in this repository and, where noted, against the local `medical_quality` database on 2026-10-28 (MySQL 8.4.13, `REPEATABLE-READ`).

## 1. Final decision

Approved for implementation.

There is no open design question. The items below are part of the same change. A build that grants the old wider allowlist, or that adds custom roles without the scope checks named here, is not this design.

## 2. Remaining blockers

None left unresolved. These are required implementation constraints, highest severity first.

1. **Scope is missing on four existing operations.** A custom role must not receive `MANAGE_CAPA`, `MANAGE_EVIDENCE`, `MANAGE_FINDINGS`, or `MANAGE_IMPORT` until those methods reject out-of-scope ids. Details and file references are in section 3.
2. **Six allowlist entries are not enforced anywhere.** They come off the allowlist. Granting them would not delegate a real operation.
3. **`roles.name` is globally unique today.** Copies cannot be inserted until `roles_name_key` is dropped, and `RoleSeedService` plus `OrganizationProvisioningService` must look up roles by `role_code` and organization in the same release. The old code cannot run after that unique index is gone and duplicate names exist.
4. **V10 is not safe to re-run** after a failed attempt. MySQL commits each DDL statement on its own. Recovery is restore or a forward-fix, not Flyway retry.

## 3. Confirmed issues

### Permissions that are enforced and stay delegable

| Permission | Enforcement | Scope on that path |
| --- | --- | --- |
| `CONDUCT_AUDIT` | `AuditController` create, score, comment, bulk, clear, submit, status. `MrReviewController` create, answer, patient, reset. | Audits: `AuditAccess.require` / `requireEditable` and `AuditCommandService.applyDepartment`. Medical-record reviews: organization only, in `MrReviewService.require`. Reviews store a department name, not `department_id`. An assessor with `CONDUCT_AUDIT` can already open every review in the hospital. A custom role with that permission gets the same hospital-wide review access. That is current behavior, not a new bypass. |
| `MANAGE_FINDINGS` | `OperationsController.openFinding` → `FindingService.open` | The UI always sends an audit id (`static/js/gsr.js`). `open` copies the department from that audit only when `auditAccess.require` succeeds. A null `auditId` still saves a finding with a null department. Live data has 0 such findings. The implementation rejects a null audit id. |
| `MANAGE_CAPA` | `OperationsController.addCapa` and `updateCapa` | **Gap.** `FindingService.addCapa` and `FindingService.updateCapa` load by id and do not call `DepartmentScope`. Any holder can change another department’s or another hospital’s CAPA. Required fix in those two methods: the finding’s department must be in `departmentIds`, otherwise not-found. |
| `MANAGE_EVIDENCE` | `OperationsController.uploadEvidence` → `EvidenceService.upload` | Upload checks `departmentIds` when an audit id or department id is present. Both are optional (`evidence-new.html`). A file with neither is stored and then hidden from `list`, while `download` still serves it. **Gap.** `EvidenceService.download` has no scope check. `GET /api/files/{documentId}` is only authenticated. Required fix: upload requires an in-scope audit or department; download resolves the evidence row and uses the same `visible` check as `list`. |
| `VIEW_COMMAND_CENTER`, `VIEW_EXECUTIVE_DASHBOARD`, `VIEW_ASSESSOR_DASHBOARD`, `VIEW_QUALITY_DIRECTOR_DASHBOARD` | `OperationsController.requireDashboard` | Any one of the four admits the caller. `DashboardService.summary` then filters audits with `canSee` and findings, CAPA, and evidence with `departmentIds`. The four names do not grant different rows. They stay on the allowlist because they are enforced and the summary is scoped. |
| `MANAGE_IMPORT` | `OperationsController.uploadImport`, `updateImport`, `confirmImport` | Confirming onto an audit calls `AuditScoreService.saveScore`, which calls `AuditAccess.requireEditable`, so an out-of-scope audit id fails and the transaction rolls back. **Gap.** `AuditImportService.updatePreview` does not check who uploaded the import or which hospital they belong to. `audit_imports` has `uploaded_by_user_id` and no `organization_id`. Required fix: update and confirm allow the row only when the uploader’s organization is the acting hospital. |
| `ARCHIVE_AUDIT` | `AuditController.archive` and `restore` → `AuditCommandService.archive` | `AuditAccess.require` before the flag changes. |
| `EDIT_AUDIT_LIFECYCLE` | `AuditController.update` → `AuditCommandService.update` | `requireEditable`, which also refuses a locked status. Department changes go through `applyDepartment`. |
| `DELETE_AUDIT_PDF`, `DELETE_AUDIT_ONLY`, `DELETE_AUDIT_EVERYTHING` | `OperationsController.remove` → `AuditRemovalService.remove` | `requirePermission` selects the mode, then `auditAccess.require` applies department scope. `overrideCapa` skips the open-CAPA warning and does not skip scope. These stay delegable. They destroy data only inside departments the caller can already see. |

`DELETE_AUDIT_EVERYTHING` soft-deletes the audit, its scores, comments, findings, CAPAs, and the stored PDF (`AuditRemovalService.remove` and `closeChildren`). That is safe to delegate only because of `auditAccess.require`. Do not move the permission check above a path that skips `require`.

### Permissions removed from the allowlist

No server check references these names. `HasPermission` and `PermissionEvaluator` are the only enforcement mechanisms, and nothing calls them for:

- `MANAGE_DEPARTMENTS` — the only annotation is `POST /api/departments`, and `HospitalQueryService.createDepartment` still calls `requireManager`. The permission does not create a department.
- `MANAGE_ASSESSORS`
- `GENERATE_REPORTS`
- `MANAGE_NOTIFICATIONS`
- `MANAGE_SETTINGS`
- `MANAGE_EXPORT`

Leaving them on the allowlist would show a grant that does not change what the user can do. They stay on the built-in roles that already have them. Custom roles cannot receive them.

`VIEW_ALL_ASSESSORS_DATA` stays off the allowlist. Its only reader is `DepartmentScope.seesAll`.

`MANAGE_USERS` and the five role-admin permissions stay off the allowlist. Their only endpoint is `RoleController`, which is the platform catalog, and the hospital directory does not consult them.

`RESET_DATA`, `VIEW_SYSTEM_LOGS`, `MANAGE_BACKUP`, and `MANAGE_RESTORE` have no endpoint. They stay off the allowlist so a hospital role cannot hold a platform-shaped permission.

### Actor ceiling

Unchanged, and still a reject rather than a silent drop. Hospital Owner may grant only the intersection of the allowlist and the permissions on that owner’s built-in role. The seeded owner does not have `CREATE_ROLE`, `VIEW_ROLES`, `UPDATE_ROLE`, `DELETE_ROLE`, `VIEW_PERMISSIONS`, or `RESET_DATA` (`RoleSeedService.roleKeys`). Platform admin may grant the allowlist only, not the rest of the platform set.

### Scope paths that already hold

| Operation | Method | Rule |
| --- | --- | --- |
| Audit list | `AuditQueryService.list` | `DepartmentScope.canSee` |
| Audit detail, scores, submit | `AuditAccess.require` / `requireEditable` | `canSee`; null department is invisible |
| Create or move an audit | `AuditCommandService.applyDepartment` | Department id must be in `departmentIds` |
| Finding list, CAPA list | `FindingService.list`, `capas` | Department id in `departmentIds` |
| Evidence list | `EvidenceService.list` | `visible` |
| Dashboard counts | `DashboardService.summary` | `canSee` and `departmentIds`, department count by acting hospital |
| Medical-record list and detail | `MrReviewService.list`, `require` | Acting hospital. Not department scope. |
| Department list | `HospitalQueryService.departments` | Acting hospital. Any signed-in user of that hospital can read it. That stays. |

`scope_mode = HOSPITAL` is visibility only. It does not add `CONDUCT_AUDIT` or any other permission. A custom role with hospital scope and an empty permission list cannot be saved; the list is non-empty and every name must be on the allowlist.

`DEPARTMENT`, `DEPARTMENT_LIST`, and `ASSESSOR` read `departmentIds`. That set is the acting hospital’s departments intersected with the user’s assignment (`DepartmentScope.departmentIds`). A department id from another hospital fails `applyDepartment`, evidence upload, and, after the required fixes, CAPA, finding create, and file download.

Changing `scope_mode` while any user has that role is rejected.

Changing a user’s role replaces `assessor_id`, `department_id`, and `user_department_access` in the same transaction that changes `role_id`. `DepartmentScope.assigned` prefers the assessor link over `department_id`. Leaving the old assessor id in place would keep the old departments. Clear the links that the new scope does not use before commit. Other transactions do not see the uncommitted mix (`open-in-view` is false).

`VIEW_ALL_ASSESSORS_DATA` for a built-in role still means what `seesAll` means today, looked up by that user’s `role_id` rather than by display name. Custom roles never take that branch.

## 4. Final custom-role allowlist

A custom hospital role may receive only:

- `CONDUCT_AUDIT`
- `MANAGE_FINDINGS`
- `MANAGE_CAPA`
- `MANAGE_EVIDENCE`
- `VIEW_COMMAND_CENTER`
- `VIEW_EXECUTIVE_DASHBOARD`
- `VIEW_ASSESSOR_DASHBOARD`
- `VIEW_QUALITY_DIRECTOR_DASHBOARD`
- `MANAGE_IMPORT`
- `ARCHIVE_AUDIT`
- `EDIT_AUDIT_LIFECYCLE`
- `DELETE_AUDIT_PDF`
- `DELETE_AUDIT_ONLY`
- `DELETE_AUDIT_EVERYTHING`

Justification for the cuts: section 3. `MANAGE_SETTINGS`, `MANAGE_EXPORT`, and the other unused names are not safe to delegate because they are not enforced, so they are not on the list. The destructive audit permissions stay because `AuditRemovalService.remove` checks the mode and then `AuditAccess.require`.

Any other name, including a name the actor holds, fails the request with `error.role.permission_forbidden` and writes nothing.

## 5. Final scope and authorization rules

Directory reads and writes, including `GET`, use `requireDirectoryAdmin`: platform admin with that hospital open, or built-in `HOSPITAL_OWNER` of that hospital. The path organization id must equal `HospitalContext.require`.

`requireManager` uses `role_code = HOSPITAL_OWNER`, built-in, same organization. Display name is not consulted. `POST /api/departments` keeps both `@HasPermission(MANAGE_DEPARTMENTS)` and `requireManager`.

`PermissionEvaluator.hasPermission` loads the signed-in user’s `role_id`. `JwtAuthenticationFilter` may keep `ROLE_` plus the display name. No controller uses `@HasRole`.

Platform routes stay `GET/POST /api/roles` and `GET /api/roles/permissions`. `GET /api/roles` returns the system role and the six templates only. `POST /api/roles` still creates a platform role and still rejects a reserved `role_code`.

Hospital routes stay under `/api/organizations/{organizationId}/users` and `.../roles`, all behind `requireDirectoryAdmin`.

Data scope:

| `scope_mode` | Visibility | Extra fields |
| --- | --- | --- |
| `HOSPITAL` | Every department of the acting hospital | None |
| `ASSESSOR` | Departments of the linked assessor, inside the hospital | Assessor in this hospital with no login |
| `DEPARTMENT` | `users.department_id` | One department in this hospital |
| `DEPARTMENT_LIST` | `user_department_access` | One or more departments in this hospital |

Built-in scope is fixed: owner, quality director, and quality supervisor `HOSPITAL`; assessor `ASSESSOR`; coordinator `DEPARTMENT`; view only `DEPARTMENT_LIST`. For those roles, `seesAll` remains the `VIEW_ALL_ASSESSORS_DATA` check by `role_id`. For a custom role, `seesAll` is true only when `scope_mode` is `HOSPITAL`.

Organization ids on create come from `HospitalContext.require`, not from the body.

## 6. Organization constraints

Live column types are signed `bigint` on `users.role_id`, `users.organization_id`, `users.assessor_id`, `users.department_id`, `roles.id`, `assessors.id`, `assessors.organization_id`, `departments.id`, `departments.organization_id`, and both `user_department_access` foreign keys. Length and signedness match, so composite foreign keys are valid in this MySQL.

`users.organization_id` and `users.role_id` are nullable. InnoDB’s default `MATCH SIMPLE` skips a composite foreign key when any referencing column is null. The super admin’s null organization is therefore not checked against a hospital role. A hospital user with both columns set is checked. `users.role_id` is null on 0 live rows.

Add:

- unique `(id, organization_id)` on `roles`, `users`, `assessors`, and `departments` as the referenced keys
- `users (role_id, organization_id)` → `roles (id, organization_id)`
- `users (assessor_id, organization_id)` → `assessors (id, organization_id)`
- `users (department_id, organization_id)` → `departments (id, organization_id)`
- `user_department_access.organization_id bigint not null`, then both pairs to `users` and `departments`
- unique `users.assessor_id` (many nulls allowed)
- check `(is_super_admin = 1 OR organization_id IS NOT NULL)`
- check `(assessor_id IS NULL OR organization_id IS NOT NULL)`
- check `(department_id IS NULL OR organization_id IS NOT NULL)`

`role_permissions.role_id` already references `roles.id`. It needs no organization column. A permission row cannot point at another hospital except by pointing at that hospital’s role, and the user-role key forbids that assignment.

Audits, findings, evidence, and imports are not given composite organization keys in this change. Their isolation stays in the services in section 3. `audit_imports` gains no new column; the hospital check uses `uploaded_by_user_id`.

`roles_name_key` on `roles.name` must be dropped before copies are inserted. Uniqueness becomes the generated `scope_key` (`role_code` plus `platform` or the organization id).

### Prechecks

Abort V10 if any query returns a row. Run on 2026-10-28, each returned count 0 except `role_count` 7 and `owner_enabled` 1.

```sql
-- duplicate role display names; copies cannot be inserted while roles_name_key exists,
-- and findByName breaks after it is dropped if this is already non-zero
SELECT name FROM roles GROUP BY name HAVING COUNT(*) > 1;

-- staff with no hospital cannot be repointed to a copy
SELECT id FROM users
WHERE is_super_admin = 0 AND organization_id IS NULL AND deleted_at IS NULL;

-- role name outside the seven seeded names
SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
WHERE r.name NOT IN (
  'System Administrator','Hospital Owner','Quality Director','Quality Supervisor',
  'Assessor','Department Coordinator','View Only');

-- super admin not on the system role, or staff on that role
SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
WHERE (u.is_super_admin = 1 AND r.name <> 'System Administrator')
   OR (u.is_super_admin = 0 AND r.name = 'System Administrator');

-- null role
SELECT id FROM users WHERE deleted_at IS NULL AND role_id IS NULL;

-- assessor or department in another hospital
SELECT u.id FROM users u JOIN assessors a ON a.id = u.assessor_id
WHERE u.organization_id IS NULL OR a.organization_id IS NULL
   OR u.organization_id <> a.organization_id;

SELECT u.id FROM users u JOIN departments d ON d.id = u.department_id
WHERE u.organization_id IS NULL OR d.organization_id IS NULL
   OR u.organization_id <> d.organization_id;

SELECT x.id FROM user_department_access x
JOIN users u ON u.id = x.user_id
JOIN departments d ON d.id = x.department_id
WHERE x.deleted_at IS NULL
  AND (u.organization_id IS NULL OR d.organization_id IS NULL
       OR u.organization_id <> d.organization_id);

-- creator's hospital differs from the audit department's hospital
SELECT a.id FROM audits a
JOIN departments d ON d.id = a.department_id
JOIN users u ON u.id = a.created_by_user_id
WHERE a.deleted_at IS NULL
  AND u.organization_id IS NOT NULL
  AND u.organization_id <> d.organization_id;
```

### Postchecks

Fail the release if any of these return a row, or if the owner count changed.

```sql
SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
WHERE u.is_super_admin = 0
  AND (r.organization_id IS NULL OR r.organization_id <> u.organization_id OR r.template = 1);

SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
WHERE u.is_super_admin = 0 AND r.role_code = 'SYSTEM_ADMINISTRATOR';

-- each hospital has six built-in copies
SELECT o.id, COUNT(*) FROM organizations o
LEFT JOIN roles r ON r.organization_id = o.id AND r.built_in = 1 AND r.template = 0 AND r.deleted_at IS NULL
WHERE o.deleted_at IS NULL
GROUP BY o.id
HAVING COUNT(r.id) <> 6;

-- copy permission count equals its template
SELECT c.id FROM roles c
JOIN roles t ON t.role_code = c.role_code AND t.template = 1
WHERE c.organization_id IS NOT NULL AND c.built_in = 1
  AND (SELECT COUNT(*) FROM role_permissions p WHERE p.role_id = c.id AND p.deleted_at IS NULL)
   <> (SELECT COUNT(*) FROM role_permissions p WHERE p.role_id = t.id AND p.deleted_at IS NULL);

SELECT x.id FROM user_department_access x
JOIN users u ON u.id = x.user_id
JOIN departments d ON d.id = x.department_id
WHERE x.organization_id <> u.organization_id OR x.organization_id <> d.organization_id;
```

Compare enabled `HOSPITAL_OWNER` users per organization with the count taken before the migration.

## 7. Migration and rollback

Take a logical backup immediately before V10. Restoring it discards every write committed after the backup, including audits, invites, and password changes. It is the recovery when the migration is partial or when new directory rows exist. It is not a harmless undo.

Flyway records a failed version and will not run that file again until the schema history is repaired. Do not repair and re-run. The script is not idempotent: MySQL 8.4 has no `ADD COLUMN IF NOT EXISTS` here, and each `ALTER` commits by itself.

### Forward order

1. Run the prechecks. Stop on any row.
2. Add nullable columns and backfill codes, flags, and scope on the seven existing rows. Do not change `role_permissions`.
3. Drop `roles_name_key`.
4. Insert six copies per organization. `INSERT ... SELECT` the template’s permission links. Do not add or remove permissions. Quality Supervisor’s copy still has `MANAGE_USERS`.
5. Repoint each non-admin `users.role_id` to the copy with the same `role_code` and the user’s `organization_id`.
6. Fill `user_department_access.organization_id` from the user.
7. Add unique keys, checks, and composite foreign keys only after the repoint.
8. Run the postchecks.

`RoleSeedService.run` must find platform rows by `role_code` and return when that row already has permissions. `OrganizationProvisioningService.saveOwner` must attach the hospital’s `HOSPITAL_OWNER` copy, not `findByName("Hospital Owner")`. Those code changes ship in the same artifact as V10. Starting the previous build after step 4 throws `IncorrectResultSizeDataAccessException` from `findByName`.

### When rollback is safe

| Situation | What to do |
| --- | --- |
| Prechecks failed before any V10 statement | Nothing is migrated. Fix the rows the query returned and run again later. |
| A DDL statement failed and Flyway marked V10 failed | Do not re-run the file. Restore the pre-migration backup. That restore discards writes made after the backup. |
| V10 succeeded, the new build is not deployed, and nobody has written through the new APIs | The manual reverse script is allowed: point users back at the template with the same `role_code`, drop the new keys, delete copy permission links and copy roles, restore `roles_name_key`, drop the new columns and `password_setup_tokens`. Template permission links stay. |
| The new build has served traffic | Reverse script is not allowed. Custom roles, setup tokens, new users, and password accepts would be orphaned or dropped. Restore the backup only if losing every later write is acceptable. Otherwise forward-fix. |
| Copies exist and the old build is still what starts | Forward-fix by deploying the build that looks up `role_code`. Do not drop the copies to make the old `findByName` work while users still point at them. |

The reverse script lives outside `db/migration`.

## 8. Invitations

Current accounts with `must_change_password` keep password login. `AuthService.login` already rejects `enabled = false`. New invitees are created disabled, with a discarded random password hash, so they cannot log in before accept.

Token: 32 bytes from `SecureRandom`, only SHA-256 stored, unique `token_hash`, `expires_at` 72 hours, `used_at` null. The response contains `setupUrl` with the raw token in the URL fragment. No password field. Do not log the body, the token, the URL, or the Authorization header. `GlobalExceptionHandler` already logs stacks, not field values.

Accept is one transaction. `SELECT ... FOR UPDATE` the token row. Proceed only when `used_at` is null and `expires_at` is in the future. Set the password, set `enabled`, set `used_at`. A second concurrent call waits, then sees `used_at` and fails. Check the update count rather than assuming the lock was the only writer.

Issuing another setup link for the same user, in that same transaction, sets `used_at` on every older unused token before inserting the new hash.

`must_change_password` blocks authenticated APIs except `GET /api/auth/me` and `POST /api/auth/password`. The password page already calls those. `POST /api/auth/invitations/accept` is public, next to login. Disabled invitees never reach that flag; they use accept.

`OrganizationProvisioningService.inviteOwner` switches to this token for new owner invites. People who already hold a temporary password are unchanged and are not printed again.

## 9. Last Hospital Owner

The database isolation is `REPEATABLE-READ`. The guard runs inside the service `@Transactional` method, which is the transaction boundary (`open-in-view` is false).

Lock the organization row, then lock the enabled built-in `HOSPITAL_OWNER` user rows:

```sql
SELECT id FROM organizations WHERE id = ? FOR UPDATE;
SELECT u.id FROM users u
JOIN roles r ON r.id = u.role_id
WHERE u.organization_id = ?
  AND r.role_code = 'HOSPITAL_OWNER' AND r.built_in = 1
  AND u.enabled = 1 AND u.deleted_at IS NULL
FOR UPDATE;
```

Refuse disable, role change, and soft-delete when the locked set would no longer contain an enabled owner. Two concurrent demotions serialize on those locks. Self-demotion uses the same method.

No current endpoint disables or reassigns an owner. `OrganizationProvisioningService.saveOwner` only inserts an enabled owner. `OwnerController` is `GET /api/owners` and `POST /api/owners`. The new user update is the only mutator, and it must call this guard. A later endpoint that sets `enabled`, `role`, or `deletedAt` on a user must call it too.

## 10. Implementation and test order

1. V10 prechecks, schema, copies, repoint, postchecks, and the `role_code` lookups in `RoleSeedService` and `OrganizationProvisioningService`. Deploy together. Tests: existing Ain Shams owner still sees the hospital; `GET /api/roles` still returns seven platform rows; `RoleControllerTest.superAdminCreatesRole` still hits `POST /api/roles`; two roles with the same display name do not share permissions.
2. `PermissionEvaluator` by `role_id`, `requireManager` / `requireDirectoryAdmin` by `role_code`, and `roleCode` on `/api/auth/me`. Tests: quality director and quality supervisor receive 403 on the new directory paths; hospital A cannot call hospital B.
3. Scope fixes before any custom role can be saved: `FindingService.addCapa`, `FindingService.updateCapa`, `FindingService.open`, `EvidenceService.upload`, `EvidenceService.download`, `AuditImportService.updatePreview` and `confirm`. Tests: a department-scoped user receives 403 or not-found for another department’s id on list, detail, create, update, upload, download, and delete.
4. Custom-role allowlist rejection, including names the owner does not hold and `DELETE_AUDIT_EVERYTHING` only inside scope. Test that a forbidden name writes no row.
5. Setup tokens, single-use under two concurrent accepts, resend invalidates the previous token, and `must_change_password` can call only `me` and `password`.
6. Last-owner lock test: two concurrent demotions leave one enabled `HOSPITAL_OWNER`.
7. Role change clears the previous assessor and department links in the same transaction. Scope change with a user assigned is rejected.

## 11. What did not change

No code, migration, configuration, or test file was edited for this review. The only update is this document.
