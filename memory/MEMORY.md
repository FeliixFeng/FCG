# Project Memory: FCG (Family Care Guardian)

## Project Type
Full-stack graduation project (毕业设计). Spring Boot 3.2.5 + Vue 3.

## Key Files
- `CLAUDE.md` — project guidance (created 2026-02-26)
- `AGENTS.md` — original dev guidelines (may be outdated re: "next steps")
- `fcg-docs/` — DATABASE_DESIGN.md, HANDOVER.md, TODO.md

## Architecture Notes
- Backend base package: `com.ghf.fcg`
- All API responses use `R<T>` wrapper from `common/result/R.java`
- Local secrets in `application-local.yml` (gitignored)
- 3 user roles: Admin=0, Member=1, Controlled Member=2
- All tables use soft delete (`deleted` field)

## User Preferences
- No special preferences recorded yet
