#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
API 冒烟测试（CI 用）。

用途：服务全部起来后跑一遍，验证「登录 + 权限 + 各模块列表 + 主链路关键节点」没坏。
不依赖第三方库（只用标准库），任何有 python3 的环境都能跑。

用法：
    python scripts/smoke_test.py                    # 默认 http://localhost:8080
    python scripts/smoke_test.py http://host:8080   # 指定网关地址
    python scripts/smoke_test.py --write            # 额外跑写操作（会建/删数据）

退出码：0 全通过；1 有失败（CI 据此判定）
"""
import json
import sys
import urllib.error
import urllib.request

DEFAULT_BASE = "http://localhost:8080"
ADMIN = ("admin", "123456")

# 各模块的列表接口（GET，只读，无副作用）
GET_ENDPOINTS = [
    ("系统-用户列表", "/api/system/user/list?page=1&pageSize=1"),
    ("系统-角色列表", "/api/system/role/list?page=1&pageSize=1"),
    ("系统-组织概览", "/api/system/org/overview"),
    ("系统-审计日志", "/api/system/audit/list?page=1&pageSize=1"),
    ("系统-配置列表", "/api/system/config/list"),
    ("系统-字典类型", "/api/system/dict/types"),
    ("CRM-客户", "/api/crm/customer/list?page=1&pageSize=1"),
    ("CRM-线索", "/api/crm/lead/list?page=1&pageSize=1"),
    ("CRM-商机", "/api/crm/deal/list?page=1&pageSize=1"),
    ("CRM-订单", "/api/crm/order/list?page=1&pageSize=1"),
    ("CRM-工作台", "/api/crm/workspace/overview"),
    ("WMS-物料", "/api/wms/material/list?page=1&pageSize=1"),
    ("WMS-仓库", "/api/wms/warehouse/list"),
    ("WMS-库位", "/api/wms/location/list"),
    ("WMS-库存", "/api/wms/inventory/list"),
    ("WMS-入库单", "/api/wms/inbound/list"),
    ("WMS-出库单", "/api/wms/outbound/list"),
    ("WMS-采购订单", "/api/wms/purchase-order/list"),
    ("WMS-盘点单", "/api/wms/stocktaking/list"),
    ("WMS-预警", "/api/wms/warning/list"),
    ("WMS-总览", "/api/wms/overview"),
    ("MAINT-设备", "/api/maint/equipment/list?page=1&pageSize=1"),
    ("MAINT-工单", "/api/maint/work-order/list?page=1&pageSize=1"),
    ("MAINT-计划", "/api/maint/plan/list?page=1&pageSize=1"),
    ("MAINT-备件", "/api/maint/spare-part/list?page=1&pageSize=1"),
    ("MAINT-工作台", "/api/maint/workspace/overview"),
    ("ETS-来货登记", "/api/ets/register/list?page=1&pageSize=1"),
    ("ETS-测试", "/api/ets/test/list?page=1&pageSize=1"),
    ("ETS-报告", "/api/ets/report/list?page=1&pageSize=1"),
    ("ETS-工作台", "/api/ets/workspace/overview"),
    ("FIN-应收", "/api/fin/receivable/list?page=1&pageSize=1"),
    ("FIN-应付", "/api/fin/payable/list?page=1&pageSize=1"),
    ("FIN-付款", "/api/fin/payment/list?page=1&pageSize=1"),
    ("FIN-费用", "/api/fin/expense/list?page=1&pageSize=1"),
    ("FIN-预算", "/api/fin/budget/list"),
    ("FIN-工作台", "/api/fin/workspace"),
    ("FIN-业务流程关联", "/api/fin/business-flow/relations"),
]


class Client:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.token = None

    def call(self, method, path, body=None, token=None, raw=None):
        url = self.base + path
        data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
        req = urllib.request.Request(url, data=data, method=method)
        req.add_header("Content-Type", "application/json; charset=utf-8")
        auth = token if token is not None else self.token
        if auth:
            req.add_header("Authorization", "Bearer " + auth)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                return resp.status, json.loads(resp.read().decode("utf-8", "replace"))
        except urllib.error.HTTPError as exc:
            payload = exc.read().decode("utf-8", "replace")
            try:
                return exc.code, json.loads(payload)
            except Exception:
                return exc.code, {"raw": payload[:200]}
        except Exception as exc:  # 连接失败等
            return -1, {"raw": repr(exc)}

    def login(self, username, password):
        status, payload = self.call("POST", "/api/auth/login",
                                    {"username": username, "password": password}, token="")
        if status == 200 and payload.get("code") == 0:
            return payload["data"]["accessToken"], payload["data"]
        return None, payload


class Report:
    def __init__(self):
        self.passed = 0
        self.failed = []

    def check(self, name, ok, detail=""):
        if ok:
            self.passed += 1
            print(f"  PASS  {name}")
        else:
            self.failed.append((name, detail))
            print(f"  FAIL  {name}  {detail}")

    def summary(self):
        total = self.passed + len(self.failed)
        print(f"\n{'=' * 60}")
        print(f"通过 {self.passed}/{total}")
        if self.failed:
            print("失败项：")
            for name, detail in self.failed:
                print(f"  - {name}: {detail}")
        print("=" * 60)
        return len(self.failed) == 0


def run(base, with_write=False):
    report = Report()
    client = Client(base)

    print("\n[1] 认证")
    token, data = client.login(*ADMIN)
    report.check("管理员登录", token is not None, str(data)[:120])
    if not token:
        return report.summary()
    client.token = token
    report.check("返回权限码", bool(data.get("permissions")), str(data.get("permissions")))
    report.check("返回角色", bool(data.get("roles")), str(data.get("roles")))

    print("\n[2] 鉴权拦截")
    status, _ = client.call("GET", "/api/wms/material/list", token="")
    report.check("无 token 访问受保护接口 -> 401", status == 401, f"实际 {status}")
    status, _ = client.call("GET", "/api/wms/material/list", token="garbage.token")
    report.check("坏 token -> 401", status == 401, f"实际 {status}")
    status, _ = client.call("GET", "/api/auth/profile", token="")
    report.check("/auth/profile 未登录 -> 401", status == 401, f"实际 {status}")

    print("\n[3] 框架错误响应体")
    status, payload = client.call("GET", "/api/system/user/role-codes")
    report.check("缺参返回带 code 的 ApiResult",
                 status == 400 and payload.get("code") == 400,
                 f"实际 {status} {str(payload)[:100]}")

    print("\n[4] 各模块列表接口")
    for name, path in GET_ENDPOINTS:
        status, payload = client.call("GET", path)
        ok = status == 200 and payload.get("code") == 0
        report.check(name, ok, f"{status} {str(payload.get('message'))[:60]}")

    print("\n[5] 响应契约（列表统一 rows/total）")
    for name, path in [("WMS-出库单", "/api/wms/outbound/list"),
                       ("WMS-入库单", "/api/wms/inbound/list"),
                       ("CRM-订单", "/api/crm/order/list")]:
        status, payload = client.call("GET", path)
        body = payload.get("data") or {}
        ok = isinstance(body, dict) and "rows" in body and "total" in body
        report.check(f"{name} 返回 rows/total", ok, f"实际 keys={list(body)[:6] if isinstance(body, dict) else type(body).__name__}")

    print("\n[6] 时间格式宽松解析")
    status, payload = client.call("GET", "/api/maint/equipment/list?page=1&pageSize=1")
    rows = (payload.get("data") or {}).get("rows") or []
    if rows:
        equipment_id = rows[0].get("id")
        for label, value in [("ISO 带T", "2026-10-26T11:00:00"),
                             ("空格无秒", "2026-10-26 11:00"),
                             ("纯日期", "2026-10-26")]:
            status, payload = client.call("POST", "/api/maint/work-order/create",
                                          {"title": f"smoke-{label}", "equipmentId": equipment_id,
                                           "reportTime": value})
            ok = status == 200 and payload.get("code") == 0
            report.check(f"工单时间格式 {label}", ok, str(payload.get("message"))[:80])
            if ok:
                client.call("POST", "/api/maint/work-order/delete", {"id": payload["data"]["id"]})
    else:
        report.check("设备种子数据存在（时间格式测试前置）", False, "设备表为空")

    print("\n[7] 审计日志")
    status, payload = client.call("GET", "/api/system/audit/list?page=1&pageSize=50")
    body = payload.get("data") or {}
    rows = body.get("rows") or body.get("list") or []
    login_logged = any(r.get("actionType") == "LOGIN" for r in rows)
    report.check("登录动作被记录到审计", login_logged, f"total={body.get('total')}")

    if with_write:
        print("\n[8] 写操作 + 审计（--write）")
        status, payload = client.call("POST", "/api/wms/brand/create",
                                      {"name": "smoke-brand", "code": "SMOKE"})
        ok = status == 200 and payload.get("code") == 0
        report.check("创建品牌", ok, str(payload.get("message"))[:80])
        if ok:
            brand_id = payload["data"]["id"]
            status, payload = client.call("POST", "/api/wms/brand/delete", {"id": brand_id})
            report.check("删除品牌", status == 200 and payload.get("code") == 0, str(payload)[:80])
        status, payload = client.call("GET", "/api/system/audit/list?page=1&pageSize=20")
        body = payload.get("data") or {}
        rows = body.get("rows") or body.get("list") or []
        has_create = any(r.get("actionType") == "CREATE" and r.get("moduleCode") == "WMS" for r in rows)
        report.check("写操作被记录到审计", has_create, "")

    return report.summary()


def main():
    args = [a for a in sys.argv[1:]]
    with_write = "--write" in args
    args = [a for a in args if a != "--write"]
    base = args[0] if args else DEFAULT_BASE
    print(f"smoke test against {base} (write={with_write})")
    ok = run(base, with_write)
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
