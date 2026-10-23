# Medical Quality

Quality platform for more than one hospital. A platform super admin creates organizations and invites their owners. An owner signs in and works inside that hospital: GSR audits, medical-record reviews, findings, CAPA, evidence, and dashboards.

The UI is Thymeleaf and jQuery. APIs are JSON and require a JWT.

## Stack

- Java 21, Spring Boot 3.5.6
- Spring Security, stateless JWT
- Thymeleaf, jQuery
- MySQL 8.4, Flyway, JPA (`ddl-auto=validate`)
- PDFBox for text PDFs

## Run

Copy the example environment and change the secrets before anything shared:

```bash
copy .env.example .env
docker compose up -d --build
```

Open [http://localhost:8080](http://localhost:8080). Health is at `/actuator/health`.

The super admin username and password come from `SUPER_ADMIN_USERNAME` and `SUPER_ADMIN_PASSWORD` in `.env`. That account is created on first start.

Uploaded files are stored under `uploads` (`APP_STORAGE_PATH`).

### Without Docker

Run MySQL 8.4, export the variables from `.env`, and set `MYSQL_HOST` to the database host. Then:

```bash
mvn spring-boot:run
```

Tests:

```bash
mvn test
```

## Who sees what

| Account | Sidebar |
| --- | --- |
| Super admin | Organizations, Owners |
| Super admin after **Open dashboard** | That hospital's workspace, with a bar to return to the platform |
| Organization owner | Home, dashboards, audits, records, departments, assessors, findings, CAPA, evidence, PDF import |

Creating an organization or inviting an owner is its own page. The organization type is a dropdown. **Hospital** is the only type for now.

The owner signs in with their email. If the password was generated, the first sign-in asks for a new one. The temporary password is shown once on screen; the app does not send mail.

## Quality work

**GSR.** Compliance is `round((met + partial × 0.5) / (total − na) × 100)`. N/A leaves the denominator. An unscored element stays in it. Clicking the selected score clears it.

**Medical records.** An item is fully compliant only when it is legible, accurate, and complete. N/A is excluded. Overall compliance is compliant items divided by applicable items.

**PDF import.** A text PDF can be parsed into a checklist preview and confirmed into an audit. Scanned PDFs are not read.

## Roles

Seeded roles: System Administrator, Quality Director, Quality Supervisor, Assessor, Department Coordinator, View Only, and Hospital Owner. The platform super admin bypasses permission checks.
