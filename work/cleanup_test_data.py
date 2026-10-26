#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""清理本次流程测试产生的数据（只删带 FLOW / 流程测试 标记的记录）"""
import json, urllib.request, urllib.error

BASE = 'http://localhost:8080/api'
TOKEN = None


def call(m, p, b=None):
    r = urllib.request.Request(BASE + p, data=json.dumps(b).encode() if b is not None else None, method=m)
    r.add_header('Content-Type', 'application/json')
    if TOKEN:
        r.add_header('Authorization', 'Bearer ' + TOKEN)
    try:
        with urllib.request.urlopen(r, timeout=30) as x:
            return json.loads(x.read())
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read())
        except Exception:
            return {}


def rows(p):
    d = call('GET', p).get('data')
    if isinstance(d, list):
        return d
    if isinstance(d, dict):
        return d.get('rows') or d.get('list') or []
    return []


def has(row, *keys):
    s = json.dumps(row, ensure_ascii=False)
    return any(k in s for k in keys)


TOKEN = call('POST', '/auth/login', {'username': 'admin', 'password': '123456'})['data']['accessToken']
MARK = ('流程测试', 'FLOW', 'OUT-MANUAL', 'SO20261026', 'PO20261026', 'IN-PO20261026')

deleted = {}


def del_list(label, list_path, delete_path, id_key='id', extra_filter=None):
    n = 0
    for r in rows(list_path):
        if not has(r, *MARK):
            continue
        if extra_filter and not extra_filter(r):
            continue
        rid = r.get(id_key)
        if rid is None:
            continue
        call('POST', delete_path, {'id': rid})
        n += 1
    deleted[label] = n


# 顺序：先删下游单据，再删上游
del_list('出库单', '/wms/outbound/list', '/wms/outbound/delete')
del_list('入库单', '/wms/inbound/list', '/wms/inbound/delete')
del_list('盘点单', '/wms/stocktaking/list', '/wms/stocktaking/delete')
del_list('采购订单', '/wms/purchase-order/list', '/wms/purchase-order/delete')
del_list('CRM订单', '/crm/order/list', '/crm/order/delete')
del_list('商机', '/crm/deal/list', '/crm/deal/delete')
del_list('线索', '/crm/lead/list', '/crm/lead/delete')
del_list('工单', '/maint/work-order/list', '/maint/work-order/delete')
del_list('保养计划', '/maint/plan/list', '/maint/plan/delete')
del_list('设备', '/maint/equipment/list', '/maint/equipment/delete')
del_list('测试报告', '/ets/report/list', '/ets/report/delete')
del_list('测试任务', '/ets/test/list', '/ets/test/delete')
del_list('来货登记', '/ets/register/list', '/ets/register/delete')
del_list('客户', '/crm/customer/list', '/crm/customer/delete')

print(json.dumps(deleted, ensure_ascii=False, indent=1))
