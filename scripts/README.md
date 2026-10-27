# QWS 启动脚本

## 一键启动

双击 `start-all.bat`，或在 PowerShell 中运行：

```powershell
.\start-all.ps1
```

默认**并行**拉起全部后端服务（8 个服务各自约 7~10s，并行总耗时约 50s；串行则要 80s+）。
已运行的服务会自动跳过。Nacos 最先启动（服务发现需要它）；网关与业务服务一起并行拉起，
网关的 `lb://` 路由是运行时从 Nacos 解析的，不依赖先起后起。
前端的启动在各后端就绪之后。

可选参数：

```powershell
.\start-all.ps1 -SkipNacos -SkipFrontend
.\start-all.ps1 -TimeoutSeconds 120
.\start-all.ps1 -Serial              # 改成串行启动，日志顺序更清晰（排查时用）
```

## 就绪判定

所有启动脚本用 `/actuator/health` 判断服务**真正就绪**，而不是只看端口是否监听——
Tomcat 会先绑定端口、再完成 Spring 上下文初始化，中间这段时间前端打过来的请求会 502/500。

> **注意（踩过的坑）**：PowerShell 5.1 对 actuator 返回的
> `Content-Type: application/vnd.spring-boot.actuator.v3+json` 这类"未知 `+json`"**不会按文本解码**，
> `$resp.Content` 会退化成 `byte[]`，此时 `$resp.Content -match '"UP"'` **恒为假**。
> 结果就是服务 9s 就 Ready，脚本却白等满 90s 才放行，看起来像"启动慢"，而且因为最终健康
> 还不会报错，极易误判。
> 正确写法是先转字符串：
>
> ```powershell
> $body = $resp.Content
> if ($body -is [byte[]]) { $body = [System.Text.Encoding]::UTF8.GetString($body) }
> if ($resp.StatusCode -eq 200 -and $body -match '"UP"') { ... }
> ```

另外：探测失败一次约耗 2s（本机 loopback 连接被拒时 `Invoke-WebRequest` 也会等满 `TimeoutSec`），
所以轮询间隔设成 500ms、并在成功时立即 `break`，不要用固定次数的秒级轮询。

## 单服务启动脚本

这些脚本用于手动分别启动每个服务。Windows 下可直接双击 `.bat`，或在 PowerShell 中运行 `.ps1`。
所有服务用 `java -jar <service>-1.0.0-SNAPSHOT.jar` 启动，jar 由 `mvn -pl <service> -am package -DskipTests` 产出。
脚本会自动：

1. 结束仍在占用目标端口的旧进程
2. 若 fat jar 不存在，自动执行 `mvn package -DskipTests`
3. 后台启动 `java -jar`，日志输出到 `../logs/<service>.{out,err}.log`
4. 轮询目标端口，启动成功后给出 PID

每个服务从 `java -jar` 到 `/actuator/health` 返回 UP 约需 7~10s，这是 Spring 的真实启动耗时。
可选的 JVM 提速参数（实测单服务能省约 1.5s，并行时收益不明显，默认未启用）：

```
-XX:TieredStopAtLevel=1 -XX:+UseSerialGC
```

## 建议启动顺序

1. `start-nacos.bat` / `start-nacos.ps1`（网关、认证、系统管理需注册发现）
2. `start-auth.bat` / `start-auth.ps1`（认证服务，8090）
3. `start-system.bat` / `start-system.ps1`（系统管理，8091）
4. `start-crm.bat` / `start-crm.ps1`（CRM，8092）
5. `start-wms.bat` / `start-wms.ps1`（WMS，8093）
6. `start-fin.bat` / `start-fin.ps1`（Finance，8094）
7. `start-maint.bat` / `start-maint.ps1`（维保，8095）
8. `start-ets.bat` / `start-ets.ps1`（电气测试，8096）
9. `start-gateway.bat` / `start-gateway.ps1`（网关，8080）
10. `start-frontend.bat` / `start-frontend.ps1`（前端，4000）

> 单服务脚本手动点的时候按上面顺序即可。一键脚本（`start-all.ps1`）默认并行，不需要这个顺序；
> 想按顺序观察日志就用 `-Serial`。

## 端口

| 服务        | 端口 | 启动类                              |
|------------|------|-------------------------------------|
| qws-auth   | 8090 | `com.qws.auth.AuthApplication`      |
| qws-system | 8091 | `com.qws.system.SystemApplication`  |
| qws-crm    | 8092 | `com.qws.crm.CrmApplication`        |
| qws-wms    | 8093 | `com.qws.wms.WmsApplication`        |
| qws-fin    | 8094 | `com.qws.fin.FinApplication`        |
| qws-maint  | 8095 | `com.qws.maint.MaintApplication`    |
| qws-ets    | 8096 | `com.qws.ets.EtsApplication`        |
| qws-gateway| 8080 | `com.qws.gateway.GatewayApplication`|
| frontend   | 4000 | Vite dev server                     |
| nacos      | 8848 | Nacos standalone                     |

## 常见操作

- 查看日志：`Get-Content ../logs/qws-wms.out.log -Tail 50 -Wait`
- 并行启动时各服务脚本自身的输出在 `../logs/start-qws-<name>.{out,err}.log`
- 停止服务：先执行 `Get-NetTCPConnection -LocalPort <port> -State Listen`，拿到 PID 后 `Stop-Process -Id <pid> -Force`
- 强制重新打包：删除 `backend/<service>/target/<service>-1.0.0-SNAPSHOT.jar` 后再运行脚本
- 如果 PowerShell 拦截 `.ps1`，可在当前会话临时放行：`Set-ExecutionPolicy -Scope Process Bypass`