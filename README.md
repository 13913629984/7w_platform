# 七维空间信息化平台

本仓库基于现有 HTML 原型，已搭建前后端分离基础框架：

- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus
- 后端：Spring Boot 3 + Spring Cloud Gateway + MyBatis Plus
- 登录：统一认证服务 `qws-auth` 签发 JWT，前端通过网关统一访问
- 已启用子系统：系统管理、CRM、WMS
- 预留子系统：ETS、Finance、Maint，可按同样模式扩展微服务

## 目录结构

```text
frontend/                 前端 SPA
backend/                  后端 Maven 多模块
  qws-common/             公共返回对象、分页对象等
  qws-gateway/            API 网关，统一入口 8080
  qws-auth/               单点登录/认证服务 8090
  qws-system/             系统管理微服务 8091
  qws-crm/                CRM 微服务 8092
  qws-wms/                WMS 微服务 8093
system/ crm/ wms/ ...     原 HTML 原型文件
```

## 启动前提

- Node.js 18+
- JDK 17+
- Maven 3.9+

## 启动后端

也可以使用 `scripts/` 目录下的单服务脚本手动分别启动，Windows 下可直接双击 `.bat`：

```text
scripts/start-nacos.bat
scripts/start-auth.bat
scripts/start-system.bat
scripts/start-crm.bat
scripts/start-wms.bat
scripts/start-gateway.bat
scripts/start-frontend.bat
```

详细说明见 `scripts/README.md`。

分别启动网关、认证和三个业务服务：

```powershell
cd backend
mvn -pl qws-auth spring-boot:run
mvn -pl qws-system spring-boot:run
mvn -pl qws-crm spring-boot:run
mvn -pl qws-wms spring-boot:run
mvn -pl qws-gateway spring-boot:run
```

服务地址：

- 网关：`http://localhost:8080`
- 认证：`http://localhost:8090/auth`
- 系统管理：`http://localhost:8091/system`
- CRM：`http://localhost:8092/crm`
- WMS：`http://localhost:8093/wms`

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问：`http://localhost:3000`

默认账号：

- 用户名：`admin`
- 密码：`123456`

## 当前功能范围

前端已按原型目录完成菜单和页面占位：

- 系统管理：用户管理、组织管理、角色权限、审计日志、系统设置
- CRM：工作台、客户、线索、联系人、商机、报价、订单、跟进活动
- WMS：总览、物料、类型、品牌、仓库、库位、库存、入库、出库、盘点、预警、采购需求、采购订单

后端每个业务微服务已提供通用 REST 占位接口：

- `GET /{service}/modules`
- `GET /{service}/{resource}`
- `POST /{service}/{resource}`

网关统一路径：

- `/api/auth/**`
- `/api/system/**`
- `/api/crm/**`
- `/api/wms/**`

## 下一步建议

1. 接入 MySQL，并为系统用户、角色、菜单、客户、物料、库存等建立真实表结构。
2. 增加 Spring Security 资源服务，在网关或各微服务校验 JWT。
3. 将前端 `CrudPage` 占位页替换为每个模块的真实表单和业务流程。
4. 引入 Nacos/Consul 做服务注册发现，替换网关中的固定 `localhost` 路由。

