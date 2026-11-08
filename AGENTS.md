# AGENTS.md — Expense Manager

General guidance for any AI coding agent (Claude Code, Codex, Cursor, or similar) that reads, analyzes, develops, tests, or maintains this repository. Tool-specific instructions (e.g. `CLAUDE.md`) layer on top of this file and take precedence for that tool; they must not contradict the rules below.

Status tags used throughout this document:
- **[Done]** — exists in the codebase today, verified by reading the repo.
- **[Planned]** — part of the agreed roadmap, not built yet.
- **[Not implemented]** — mentioned as a project requirement but has no code yet; do not assume it exists.

## 1. Project Overview

Expense Manager is a personal finance web application. Multiple users register their own account and track their own income and expenses independently. Strict per-user data isolation is the single most important requirement of the whole project — more important than any individual feature.

Development follows a controlled "Vibe Coding" process: an AI agent assists with analysis, implementation, testing, and explanation, but a human maintainer makes business decisions, approves architectural changes, and reviews each phase before the next one starts. An agent must never treat itself as the sole decision-maker for scope or architecture.

## 2. Technology Stack

**[Done]** (confirmed from `pom.xml` and the actual runtime, not assumed):
- Java 25, Spring Boot 4.1.2 (Maven, `spring-boot-starter-parent`)
- `spring-boot-starter-webmvc` (REST API, not yet exposed by any controller)
- `spring-boot-starter-data-jpa` + Hibernate ORM
- `spring-boot-starter-validation` (Jakarta Bean Validation)
- Microsoft SQL Server (`mssql-jdbc`, runtime scope)
- Lombok (optional)
- Test-only: `spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test`, `spring-boot-starter-webmvc-test` (Spring Boot 4's modular test starters — JUnit 5, AssertJ, Mockito; **no embedded database** like H2 is on the classpath)

**[Not implemented]**: Spring Security (listed as a required technology in the project's own process document, but not yet a dependency in `pom.xml`). Do not write code that assumes a `SecurityContext`, authentication filter, or password encoder bean exists until this is added.

**[Not implemented]**: schema migration tool (Flyway/Liquibase). Schema is currently managed only by Hibernate `ddl-auto=update`.

Always verify actual versions/dependencies from `pom.xml` directly — do not rely on this document if it may be stale; re-check before assuming.

## 3. Project Architecture

Layered architecture, scaled to project size — do not introduce layers or abstractions before there is a concrete need for them:

| Layer | Responsibility | Status |
|---|---|---|
| `config` | app and security configuration | **[Not implemented]** |
| `security` | authentication, current-user resolution | **[Not implemented]** |
| `controller` | HTTP request/response | **[Not implemented]** |
| `service` | business logic, ownership checks, orchestration | **[Not implemented]** |
| `repository` | data access (Spring Data JPA) | **[Done]** — 3 repositories |
| `entity` | JPA entity mapping | **[Done]** — 3 entities + 1 enum |
| `dto` | API request/response shapes | **[Not implemented]** |
| `exception` | centralized error handling | **[Not implemented]** |
| `static` | HTML/CSS/JS, only if frontend is served directly by Spring Boot | **[Not implemented]** |

Rules that apply once the missing layers are built:
- Business logic belongs in `service`, not in `controller` or `repository`.
- Use constructor injection.
- Return DTOs from controllers, never entities directly, whenever exposing an entity risks leaking internal data.
- Validate input with Jakarta Bean Validation.
- Use `BigDecimal` for money, `LocalDate`/`LocalDateTime` for dates.
- Use `@Transactional` where multiple data operations must be atomic.

## 4. Directory Structure

Reflects the actual repository as of this writing — **not** a target structure:

```
expense-manager/
├── pom.xml
├── CLAUDE.md
├── AGENTS.md
├── src/main/java/com/example/expense_manager/
│   ├── ExpenseManagerApplication.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Category.java
│   │   ├── Transaction.java
│   │   └── TransactionType.java
│   └── repository/
│       ├── UserRepository.java
│       ├── CategoryRepository.java
│       └── TransactionRepository.java
├── src/main/resources/
│   ├── application.properties        (committed — no secrets)
│   └── application-local.properties  (gitignored — real DB credentials)
└── src/test/java/com/example/expense_manager/
    ├── ExpenseManagerApplicationTests.java
    └── repository/
        ├── UserRepositoryTest.java
        ├── CategoryRepositoryTest.java
        └── TransactionRepositoryTest.java
```

No `config/`, `security/`, `controller/`, `service/`, `dto/`, or `exception/` packages exist yet. Before claiming a class, package, or endpoint exists, grep the repo — do not infer it from the roadmap.

## 5. Coding Standards

- Vietnamese is the project's working language for communication (explanations, reports, planning) per the human maintainer's preference recorded in tool-specific instructions. Code identifiers (classes, methods, variables, packages, API paths) stay in English, consistently.
- No speculative abstractions: no interface with a single implementation, no factory for one product, no config knob for a value that never changes.
- No class scaffolding "for later" — only build what the currently approved phase needs.
- Prefer the smallest diff that correctly solves the problem; don't rewrite working files to make an unrelated change.
- Never overwrite code written by the human maintainer without first reading and explaining the change.

## 6. Database Design Principles

- SQL Server only. Do not switch database vendors without an explicit, justified decision from the maintainer.
- Money fields: Java `BigDecimal`, mapped to SQL Server `DECIMAL(18,2)` (`@Column(precision = 18, scale = 2)`), enforced `> 0` via `@DecimalMin(value = "0.0", inclusive = false)` — **[Done]** on `Transaction.amount`.
- `User.email` is unique — **[Done]**, enforced via `@Column(unique = true)`.
- Ownership is modeled with non-nullable `@ManyToOne` relations (`Category.user`, `Transaction.user`, `Transaction.category`) — **[Done]**.
- Cross-entity rules that a single-table constraint or JPA annotation cannot express (e.g. "a transaction's category must belong to the same user", "a category's type must match its transactions' type") are **[Not implemented]** — they belong in the `service` layer, not forced into `entity` or `repository`.
- Schema is currently managed by Hibernate `ddl-auto=update` only. This is acceptable for the current development stage but must not be treated as a long-term migration strategy — proposing Flyway (or similar) requires an explicit decision from the maintainer before adoption.
- Never run destructive database operations (`DROP`, recreating a database, deleting rows to "fix" an error) without explicit approval, and never on an environment that may hold real user data.
- Connection credentials live only in `src/main/resources/application-local.properties`, which is git-ignored. `application.properties` (committed) must never contain a password, connection string with credentials, or any other secret.

## 7. Business Rules

**[Done]** (enforced today, verified by passing tests):
- Transaction amount must be `> 0` (Bean Validation, checked on persist).
- `User.email` must be unique (DB unique constraint).

**[Not implemented]** (require a `service` layer that does not exist yet):
- A transaction's type must be exactly `INCOME` or `EXPENSE` (the Java enum already restricts this at the type level, but there is no business-rule layer yet beyond that).
- A category's type must match the type of transactions assigned to it.
- A category and the transactions that reference it must belong to the same user.
- A category in use by existing transactions cannot be deleted without an approved safe-handling rule.
- Monthly/period statistics must be scoped to the correct date range and to the current user only.
- "Net balance" for a period means total income minus total expense in that period — it must never be presented as a bank balance.

All business rules must be enforced server-side. Any client-side validation is a UX convenience only.

## 8. Security Requirements

**This is the single most important non-functional requirement of this project.** Spring Security is **[Not implemented]** as of this writing — do not assume authentication, authorization, or a security context exist.

Once built, these rules are non-negotiable:
- Passwords are hashed (e.g. BCrypt via Spring Security's `PasswordEncoder`) — never stored or returned as plaintext, never logged, never present in an API response.
- The current user is resolved from the server-side security context — never trusted from a client-supplied `userId`.
- Every read, update, and delete of a transaction or category must check ownership server-side.
- A user must never be able to attach their transaction to another user's category.
- Dashboard/statistics aggregates must be scoped to the requesting user only.
- Hiding a button in the frontend is not a security control.
- Prefer repository queries that filter by both resource ID and owner in one call (see `findByIdAndUser` pattern already used in `CategoryRepository`/`TransactionRepository`) over "fetch by ID, then check ownership in application code."
- CSRF must be handled correctly for session/cookie-based state-changing requests — never disabled wholesale to work around a frontend integration issue.

Mandatory security test scenarios once auth exists (do not consider the security work done until these pass):
1. An unauthenticated request cannot reach a protected resource.
2. User A cannot read user B's transaction by guessing/changing an ID.
3. User A cannot update or delete user B's transaction.
4. User A cannot read, update, or delete user B's category.
5. User A cannot use user B's category for their own transaction.
6. User A's dashboard never contains user B's data.
7. A spoofed `userId` from the client cannot bypass ownership checks.
8. Login, logout, session, and CSRF behave as configured.

## 9. Testing Guidelines

- No embedded test database (no H2) is on the classpath. Repository tests use `@DataJpaTest` with `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` so they run against the real local development SQL Server instance; `@DataJpaTest`'s default per-test transaction rollback keeps this non-destructive.
- **Spring Boot 4.1.2 moved these annotations to new packages** — verify against the actual jars in the local Maven repo before trusting any example from Spring Boot 3 docs:
  - `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
  - `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
- Current coverage **[Done]**: 3 repository test classes (`UserRepositoryTest`, `CategoryRepositoryTest`, `TransactionRepositoryTest`), 7 tests, last verified passing via `./mvnw.cmd test`.
- Not yet testable: cross-entity business rules and all security scenarios listed above — they require a `service` layer and Spring Security that don't exist yet. State this explicitly in reports; never claim a rule is tested when only the infrastructure for it exists.
- Never report a test as passing without having actually run it and read the result.
- Never weaken a test or disable a security check just to make a build pass.

## 10. Git Workflow

- Check the current branch and working tree status before starting any task.
- Do not develop directly on `main`.
- Use feature branches, e.g. `feature/database-model`, `feature/authentication`, `feature/categories`, `feature/transactions`, `feature/dashboard`, `feature/frontend`, `test/security-hardening`.
- Never run `reset --hard`, force-push, or any other destructive/history-rewriting Git command without explicit instruction.
- Never commit or push unless the maintainer explicitly asked for it in that session.
- Never commit secrets, credentials, connection strings, or real financial data.
- Touch only the files within the scope that was explicitly approved for the current task.

## 11. AI Agent Development Workflow

Any agent working on this repository must:

1. **Read this file and any tool-specific instruction file (e.g. `CLAUDE.md`) before doing anything else.** If both exist, tool-specific instructions take precedence for that tool but must not relax the security/ownership rules in this document.
2. **Survey the actual repository state before writing code** — directory structure, `pom.xml`, existing entities/repositories/services/controllers, test suite, `git status` and current branch. Never assume a feature is missing just because it isn't on the roadmap yet, and never assume it exists just because the roadmap says it should.
3. **Work in small, independently verifiable phases.** Do not implement multiple roadmap phases in one pass. Do not silently expand scope beyond what was approved for the current task.
4. **Report actual results, not intentions.** State exactly what was built, what was tested and how, what passed, what failed, and what could not be verified and why. Never claim a test passed without having run it.
5. **Stop at phase boundaries and wait for explicit approval** before starting the next phase or widening scope.
6. **Preserve existing working configuration and code.** Do not rewrite `pom.xml` or `application.properties` wholesale, do not change database/connection settings without explaining why first, and do not overwrite human-written code without review.
7. **Never weaken security, validation, or tests to make something pass.** If a change seems to require that, stop and ask instead.
