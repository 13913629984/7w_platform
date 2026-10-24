# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

**信息化平台 (QWS Platform)** — an enterprise information management platform with multiple subsystems (CRM, WMS, Maintenance, ETS, Finance, System Admin). Built as a SPA + microservices architecture.

- **Frontend**: Vue 3 + TypeScript + Vite 6 + Pinia + Vue Router 4 + Element Plus
- **Backend**: Spring Boot 3.3 + Spring Cloud Gateway 2023.0 + MyBatis Plus 3.5 + Nacos Discovery
- **Auth**: JWT-based (jjwt 0.12), unified auth service `qws-auth`
- **Database**: MySQL, single schema `7wsys`; tables are created idempotently by each module's `*DataInitializer` (`@PostConstruct` + JdbcTemplate `CREATE TABLE IF NOT EXISTS` + seed) — no Flyway/Liquibase
- **Default login**: admin / 123456

## Directory Structure

```
frontend/               Vue 3 SPA (port 3000)
  src/
    api/request.ts       Axios instance, /api proxy to gateway :8080, Bearer token
    stores/auth.ts       Pinia auth store (login/logout/profile/permissions)
    router/index.ts      Vue Router with auth guards (route.meta.service)
    router/menus.ts      Sidebar menu definitions (permission-gated)
    layouts/AppLayout.vue Main layout (sidebar + header + router-view)
    components/CrudPage.vue Reusable CRUD placeholder component
    views/
      login/             Login page
      dashboard/         Dashboard with subsystem cards & charts
      system/            System admin (users, orgs, roles, audits, settings)
      crm/               CRM (workspace, customers, leads, contacts, deals, quotes, orders, activities)
      wms/               WMS (overview, materials, types, brands, warehouses, locations, inventory, inbound, outbound, stocktaking, warnings, purchase)
      maint/             Maintenance (workspace, equipments, work orders, plans, spare parts)
      ets/               Electrical testing (workspace, registers, tests, reports)
      fin/               Finance (workspace, receivables, payables, expenses, budgets)
backend/                Maven multi-module (JDK 17)
  qws-common/           Shared: ApiResult, entities (SysUser, SysRole, etc.), mappers, DTOs, PageQuery, PasswordUtil
  qws-gateway/          Spring Cloud Gateway (port 8080), routes to /api/auth/** /api/system/** /api/crm/** /api/wms/**
  qws-auth/             Auth service (port 8090), JWT login + profile, Nacos discovery
  qws-system/           System management (port 8091), CRUD for users/orgs/roles/permissions/audit logs
  qws-crm/              CRM (port 8092): customer/contact/deal/quote/order/lead/activity
  qws-wms/              WMS (port 8093): 24 tables, materials/inventory/inbound/outbound/stocktaking/warning/purchase/BOM
  qws-fin/              Finance (port 8094): receivable/payable/payment/expense/budget/cost/business-flow
  qws-maint/            Maintenance (port 8095): equipment/work-order/plan/spare-part
  qws-ets/              Electrical testing (port 8096): register/test/report
scripts/                Per-service start scripts (.bat + .ps1) using mvn spring-boot:run
```

## Architecture

- **Frontend → Gateway**: All API calls go through Vite proxy (`/api` → `localhost:8080`) to Spring Cloud Gateway
- **Gateway routes**: `/api/{auth,system,crm,wms,fin,maint,ets}/**` → corresponding service with `StripPrefix=1`
- **Gateway auth**: `JwtAuthFilter` (GlobalFilter, JJWT) validates the Bearer token and injects `X-User-Id` / `X-Username` / `X-User-Roles` / `X-User-Permissions` downstream. Whitelist: `/api/auth/login`, `/api/auth/profile`, `/actuator/**`. Gateway is reactive — do NOT use servlet `OncePerRequestFilter`.
- **Auth flow**: JWT issued by `qws-auth`, stored in localStorage, sent as `Authorization: Bearer <token>` via Axios interceptor
- **Auth guard**: Vue Router `beforeEach` checks token + fetches profile + validates `route.meta.service` permission
- **Mock mode**: Set `VITE_USE_MOCK=true` to bypass backend auth (dev convenience)
- **Permission model**: Role-based with subsystem codes (`SYS`, `CRM`, `WMS`, `MAINT`, `ETS`, `FIN`); admin roles (`ADMIN`, `SUPER_ADMIN`, `SYS_ADMIN`) auto-grant all
- **API convention**: Unified response wrapper `ApiResult<T>(code, message, data)` — `code: 0` = success
- **Service pattern**: every list endpoint returns `{rows,total,page,pageSize,stats}` and paginates server-side via MyBatis-Plus `selectPage`; writes use POST `/create` `/update` `/delete` (no PUT/DELETE verbs)
- **Entities**: Defined in `qws-common` with MyBatis Plus mappers — `SysUser`, `SysRole`, `SysPermission`, `SysDepartment`, `SysEmployee`, `SysAuditLog`, `SysUserRole`, `SysRolePermission`
- **PageQuery**: Shared pagination record with `current()` / `size()` helpers

## Common Commands

### Backend

```powershell
# Build all modules
cd backend && mvn clean install -DskipTests

# Build a single module
cd backend && mvn clean install -pl qws-auth -am -DskipTests

# Run a single service (with dependent modules)
cd backend && mvn -pl qws-auth -am spring-boot:run

# Run all services (PowerShell)
cd backend
mvn -pl qws-auth -am spring-boot:run  # 8090
mvn -pl qws-system -am spring-boot:run  # 8091
mvn -pl qws-crm -am spring-boot:run  # 8092
mvn -pl qws-wms -am spring-boot:run  # 8093
mvn -pl qws-fin -am spring-boot:run  # 8094
mvn -pl qws-maint -am spring-boot:run  # 8095
mvn -pl qws-ets -am spring-boot:run  # 8096
mvn -pl qws-gateway -am spring-boot:run  # 8080 (start last)
```

### Frontend

```powershell
cd frontend
npm install
npm run dev        # Dev server on http://localhost:4000
npm run build      # Production build (vue-tsc -b && vite build)
npm run preview    # Preview production build
```

### Start Scripts (Windows)

Scripts in `scripts/` folder — each has `.bat` (double-click) and `.ps1` variants:
```
scripts/start-nacos.bat       # Start Nacos (required for gateway + auth + system)
scripts/start-auth.bat        # Auth service 8090
scripts/start-system.bat      # System management 8091
scripts/start-crm.bat         # CRM 8092
scripts/start-wms.bat         # WMS 8093
scripts/start-fin.bat         # Finance 8094
scripts/start-maint.bat       # Maintenance 8095
scripts/start-ets.bat         # ETS 8096
scripts/start-gateway.bat     # Gateway 8080
scripts/start-frontend.bat    # Frontend dev server 4000
```

## Key Patterns

- **Adding a new backend microservice**: Create a new Maven module with `spring-boot-starter-web` + `qws-common` dependency, add REST controller under `/{service-name}`, add route in gateway config, add menu entry in `frontend/src/router/menus.ts` with the appropriate `permission` code
- **Adding a new frontend view**: Create a `.vue` file under the appropriate `views/{subsystem}/` directory, add a route in `router/index.ts` with `meta.service`, add a menu entry in `router/menus.ts`
- **CrudPage component**: A reusable placeholder for standard list pages (stats cards + filter bar + table) — used by most CRM/WMS/System views
