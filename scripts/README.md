# QWS 单服务启动脚本

这些脚本用于手动分别启动每个服务。Windows 下可直接双击 `.bat`，或在 PowerShell 中运行 `.ps1`。
所有服务用 `java -jar <service>-1.0.0-SNAPSHOT.jar` 启动，jar 由 `mvn -pl <service> -am package -DskipTests` 产出。
脚本会自动：

1. 结束仍在占用目标端口的旧进程
2. 若 fat jar 不存在，自动执行 `mvn package -DskipTests`
3. 后台启动 `java -jar`，日志输出到 `../logs/<service>.{out,err}.log`
4. 轮询目标端口，启动成功后给出 PID

## 建议启动顺序

1. `start-nacos.bat` / `start-nacos.ps1`（网关、认证、系统管理需注册发现）
2. `start-auth.bat` / `start-auth.ps1`（认证服务，8090）
3. `start-system.bat` / `start-system.ps1`（系统管理，8091）
4. `start-crm.bat` / `start-crm.ps1`（CRM，8092）
5. `start-wms.bat` / `start-wms.ps1`（WMS，8093）
6. `start-gateway.bat` / `start-gateway.ps1`（网关，8080）
7. `start-frontend.bat` / `start-frontend.ps1`（前端，3000）

## 端口

| 服务        | 端口 | 启动类                              |
|------------|------|-------------------------------------|
| qws-auth   | 8090 | `com.qws.auth.AuthApplication`      |
| qws-system | 8091 | `com.qws.system.SystemApplication`  |
| qws-crm    | 8092 | `com.qws.crm.CrmApplication`        |
| qws-wms    | 8093 | `com.qws.wms.WmsApplication`        |
| qws-gateway| 8080 | `com.qws.gateway.GatewayApplication`|
| frontend   | 3000 | Vite dev server                     |
| nacos      | 8848 | Nacos standalone                     |

## 常见操作

- 查看日志：`Get-Content ../logs/qws-wms.out.log -Tail 50 -Wait`
- 停止服务：先执行 `Get-NetTCPConnection -LocalPort <port> -State Listen`，拿到 PID 后 `Stop-Process -Id <pid> -Force`
- 强制重新打包：删除 `backend/<service>/target/<service>-1.0.0-SNAPSHOT.jar` 后再运行脚本
- 如果 PowerShell 拦截 `.ps1`，可在当前会话临时放行：`Set-ExecutionPolicy -Scope Process Bypass`