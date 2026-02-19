# AGENTS.md - FCG (Family Care Guardian)

> 家庭健康管理系统 - 毕业设计项目

**Generated:** 2026-02-19  
**Commit:** 43a54c2  
**Branch:** main

## Project Overview

- **Type**: Full-stack web application (Spring Boot + Vue 3)
- **Backend**: Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, Maven
- **Frontend**: Vue 3.4.21, Vite 5.2.0, npm
- **Database**: MySQL 8.0

## Build Commands

### Backend (fcg-server/)

```bash
cd fcg-server

# Compile
mvn compile

# Run tests
mvn test

# Run single test class
mvn test -Dtest=ClassName

# Run single test method
mvn test -Dtest=ClassName#methodName

# Package (creates JAR)
mvn package

# Clean build
mvn clean package

# Run application
mvn spring-boot:run

# Or run main class directly
java -jar target/fcg-server-1.0.0.jar
```

### Frontend (fcg-client/)

```bash
cd fcg-client

# Install dependencies
npm install

# Development server
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

## Code Style Guidelines

### Java (Backend)

- **Package**: `com.ghf.fcg`
- **Naming**: PascalCase classes, camelCase methods/variables, UPPER_SNAKE_CASE constants
- **Imports**: Use explicit imports, no wildcard imports
- **Lombok**: Use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` for entities
- **Annotations**: Place class annotations on separate lines
- **Braces**: Opening brace on same line (K&R style)
- **Indent**: 4 spaces, no tabs

### Vue/JavaScript (Frontend)

- **Style**: Composition API with `<script setup>`
- **Naming**: PascalCase components, camelCase functions/variables
- **Imports**: Use single quotes, no semicolons
- **Template**: 2-space indent, semantic HTML
- **Comments**: Use `//` for single line, `/* */` for blocks

### General

- **Encoding**: UTF-8
- **Line endings**: LF (Unix-style)
- **Line length**: 120 characters max
- **Trailing whitespace**: Remove before commit
- **EOF**: End files with a single newline

## Project Structure

```
fcg/
├── fcg-server/              # Spring Boot backend
│   ├── pom.xml             # Maven configuration
│   └── src/
│       ├── main/java/com/ghf/fcg/   # Java source
│       └── main/resources/          # Configs (application.yml)
│
└── fcg-client/              # Vue 3 frontend
    ├── package.json        # npm configuration
    ├── index.html
    └── src/
        ├── App.vue
        └── main.js
```

## Code Map

### Backend Entry Points

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `FcgApplication` | Class | `fcg-server/src/main/java/com/ghf/fcg/FcgApplication.java` | Spring Boot 启动类 |
| `Knife4jConfig` | Class | `fcg-server/.../config/Knife4jConfig.java` | Swagger/OpenAPI 文档配置 |
| `MybatisPlusConfig` | Class | `fcg-server/.../config/MybatisPlusConfig.java` | MyBatis-Plus 配置 |
| `GlobalExceptionHandler` | Class | `fcg-server/.../common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `R` | Class | `fcg-server/.../common/result/R.java` | 统一响应封装 |

### Frontend Entry Points

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `App` | Component | `fcg-client/src/App.vue` | 根组件 |
| `main` | Script | `fcg-client/src/main.js` | 应用入口 |

## Key Configuration Files

- **Backend**: `fcg-server/src/main/resources/application.yml`
- **Local Config**: `application-local.yml` (gitignored, contains secrets)
- **Frontend**: `fcg-client/package.json`

## Testing

### Backend Tests
- Use JUnit 5 (included with Spring Boot)
- Test classes: `src/test/java/...`
- Naming: `*Test.java` or `*Tests.java`

### Running Single Test
```bash
# Maven single test
mvn test -Dtest=UserServiceTest

# Single test method
mvn test -Dtest=UserServiceTest#shouldCreateUser
```

## Notes

- This is a graduation project (毕业设计) from Wuhan Textile University
- Backend runs on port 8080 by default
- Frontend dev server typically runs on port 5173
- Database credentials stored in `application-local.yml` (not in git)
- No existing linter configs (ESLint/Prettier) or Cursor/Copilot rules found

## Dependencies to Know

### Backend
- Spring Boot Web, Validation
- MyBatis-Plus (ORM)
- MySQL Connector
- Lombok
- Knife4j (Swagger/OpenAPI 文档)

### Frontend
- Vue 3 (Composition API)
- Vite (build tool)
- Planned: Element Plus, Axios, Pinia, Tailwind CSS

## Common Tasks

```bash
# Start backend
cd fcg-server && mvn spring-boot:run

# Start frontend (in new terminal)
cd fcg-client && npm run dev

# Full build
cd fcg-server && mvn clean package && cd ../fcg-client && npm run build
```
