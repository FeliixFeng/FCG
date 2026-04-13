# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (fcg-server/)
```bash
cd fcg-server
mvn spring-boot:run          # Run dev server (port 8080)
mvn clean package            # Build JAR
mvn test                     # Run all tests
mvn test -Dtest=ClassName    # Run single test class
mvn test -Dtest=ClassName#methodName  # Run single test method
```

### Frontend (fcg-client/)
```bash
cd fcg-client
npm run dev      # Dev server (port 5173)
npm run build    # Production build
npm run preview  # Preview production build
```

## Architecture

**FCG (Family Care Guardian)** is a family health management app — Spring Boot 3.2.5 backend + Vue 3 frontend.

### Backend (`fcg-server/`)
- Package root: `com.ghf.fcg`
- Three business modules under `modules/`: `system`, `medicine`, `health`
- Each module follows the pattern: `entity/` → `mapper/` (MyBatis-Plus) → `service/` (interface + `impl/`)
- `common/result/R.java` — unified response wrapper used by all controllers
- `common/exception/GlobalExceptionHandler.java` — global exception handling
- Local secrets (DB credentials, JWT secret, API keys) go in `application-local.yml` (gitignored)

### Frontend (`fcg-client/`)
- Composition API with `<script setup>` throughout
- Two layout components define the dual-layer interface:
  - `BaseLayout.vue` — user interface (`/` routes), all members
  - `AdminLayout.vue` — admin interface (`/admin` routes), super admin only
- State: Pinia stores; routing: Vue Router with dual-token guards (family-level + member-level)
- UI: Element Plus components, ECharts for charts, Tailwind for utilities

### Role System
Three roles control UI and API access:
- `role=0` Super Admin: full access + admin interface entry
- `role=1` Regular Member: self-management only
- `role=2` Controlled Member: forced "care mode" (large fonts, simplified UI, view-only for medicine)

Medicine library is **shared** across the family; medication plans are **per-member**.

### AI Integrations
- Medicine OCR: Zhipu AI GLM-4V-FlashX via `POST /api/medicine/ocr`
- Health report summaries: same model, triggered from health module
- Image storage: Aliyun OSS via `POST /api/oss/upload?dir=medicine`

## Code Style

**Java**: 4-space indent, K&R braces, Lombok on entities (`@Data`, `@Builder`, etc.), explicit imports only.

**Vue/JS**: 2-space indent, single quotes, no semicolons, PascalCase components, camelCase functions.

**Line length**: 120 chars max. **Encoding**: UTF-8, LF line endings.

## Git Conventions

Merges must use `--no-ff` with a descriptive message (configured via `merge.ff=false`):
```bash
git merge dev -m "Merge: [功能/修复] - 简要说明"
```

Development happens on `dev`; `main` triggers CI/CD deployment to Alibaba Cloud ACR.
