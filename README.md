# TaskFlow (Spring Boot 4)

Lightweight task orchestration platform inspired by monday.com, with Kanban board, table view, categories, controlled status transitions, and automatic archiving for old completed tasks.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring MVC + Thymeleaf
- Spring Data JPA + Hibernate
- MariaDB/MySQL-compatible connection (default) + H2 (tests)
- Bootstrap 5 + Bootstrap Icons + SortableJS + HTMX

## Features in this iteration

- Task CRUD
- Categories (seeded defaults: College, Work, Personal, Finance)
- Status workflow (`TO_DO`, `DOING`, `DONE`, `STUCK`) with state machine guard
- Kanban board (`/board`)
- Table view (`/tasks/table`)
- Archive view (`/tasks/archive`)
- Daily scheduled archive job for tasks completed for 14+ days

## Prepare database

The project includes a ready SQL script:

- `src/main/resources/db/mariadb/create_taskdb.sql`

Run it with your local MariaDB instance:

```bash
mariadb -u root -p < src/main/resources/db/mariadb/create_taskdb.sql
```

## Run locally (AjaxSolutions-style connection)

```bash
DB_URL="jdbc:mysql://localhost:3306/taskdb" \
DB_USER="root" \
DB_PASSWORD="vertrigo" \
./mvnw spring-boot:run
```

Then open:

- `http://localhost:8080/board`

## Run tests

```bash
./mvnw test
```

## Packaging notes

`pom.xml` includes the same packaging plugin pattern used in AjaxSolutions projects:

- `maven-dependency-plugin` to copy dependencies to `target/lib`
- `maven-jar-plugin` with manifest classpath and `mainClass`

## Notes

- Drag-and-drop now persists status changes automatically via backend endpoint integration.
- HTMX-style request handling is wired for Kanban drop events (`POST /tasks/{id}/status/drag`).
- Quick status update remains available as a fallback.
