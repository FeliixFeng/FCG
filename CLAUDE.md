# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FCG (Family Care Guardian / 家庭健康管理系统) is a full-stack graduation project. Backend: Spring Boot 3.2.5 + Java 17 + MyBatis-Plus + MySQL. Frontend: Vue 3 + Vite + Pinia.

## Build & Run Commands

### Backend (`fcg-server/`)
```bash
mvn spring-boot:run          # Run dev server (port 8080)
mvn clean package            # Build JAR
mvn test                     # Run all tests
mvn test -Dtest=ClassName    # Run single test class
mvn test -Dtest=ClassName#methodName  # Run single test method
```

### Frontend (`fcg-client/`)
```bash
npm install      # Install dependencies
npm run dev      # Dev server (port 5173)
npm run build    # Production build
```

### Database
```bash
mysql -h HOST -u USER -p DATABASE < fcg-server/src/main/resources/sql/init.sql
```

## Configuration

- `fcg-server/src/main/resources/application.yml` — main config
- `fcg-server/src/main/resources/application-local.yml` — local secrets (gitignored, copy from `application-local.yml.example`)
- Database credentials, OSS keys, and AI API keys go in `application-local.yml`

## Architecture

### Backend Layers
Each business module under `com.ghf.fcg.modules/` follows this structure:
- `controller/` — REST endpoints, returns `R<T>` (unified response wrapper)
- `service/` — `IXxxService` interface + `impl/XxxServiceImpl` implementation
- `mapper/` — MyBatis-Plus mapper interfaces
- `entity/` — domain models with Lombok (`@Data`, `@Builder`, etc.)
- `dto/` / `vo/` — request/response models

### Business Modules
- `system/` — users, families, JWT auth, RBAC (3 roles: Admin=0, Member=1, Controlled=2)
- `medicine/` — drug inventory, medication plans, adherence records, OCR recognition
- `health/` — vital signs (BP, glucose, HR, temp, weight), weekly health reports
- `ai/` — LLM integration for medication advice and health summaries

### Common Infrastructure (`com.ghf.fcg.common/`)
- `result/R.java` — unified API response wrapper
- `exception/GlobalExceptionHandler.java` — global exception handling
- `JwtAuthInterceptor` — JWT validation on all protected routes
- `UserContext` — ThreadLocal for current user in request scope

### Frontend Structure (`fcg-client/src/`)
- Composition API with `<script setup>` throughout
- Pinia for state management (user store)
- Axios HTTP client with JWT Bearer token injection
- Vue Router with auth guards

## Code Style

### Java
- Package root: `com.ghf.fcg`
- 4-space indent, K&R braces, 120-char line limit
- Explicit imports only (no wildcards)
- Lombok annotations on entities

### Vue/JS
- Composition API with `<script setup>`, no Options API
- Single quotes, no semicolons
- 2-space indent in templates

## Key External Services
- Aliyun OSS — image/file storage (configured via `AiProperties` / `OssConfig`)
- OCR API — drug package text recognition
- LLM API — medication advice and health summaries (configured in `application-local.yml`)
