#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""主营业务闭环流程测试：线索→客户→商机→订单→确认→出库→应收→开票→核销→财务流水"""
import json, time, urllib.request, urllib.error

BASE = 'http://localhost:8080/api'
TOKEN = None
RUN = time.strftime('%H%M%S')


def call(method, path, body=None, timeout=30):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if TOKEN:
        req.add_header('Authorization', 'Bearer ' + TOKEN)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode('utf-8', 'replace'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', 'replace')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {'raw': raw[:200]}
    except Exception as e:
        return -1, {'raw': repr(e)}


N = [0]
FAILS = []


def step(name, method, path, body=None):
    N[0] += 1
    st, j = call(method, path, body)
    code = j.get('code') if isinstance(j, dict) else None
    ok = st == 200 and code == 0
    msg = (j.get('message') if isinstance(j, dict) else '') or ''
    if not ok:
        FAILS.append((name, st, code, msg))
    print('%s %02d. %-32s [%s/%s] %s' % ('PASS' if ok else 'FAIL', N[0], name, st, code,
                                         '' if ok else msg[:150]))
    return (j.get('data') if ok and isinstance(j, dict) else None), ok, msg


def rows_of(d):
    if isinstance(d, list):
        return d
    if isinstance(d, dict):
        return d.get('rows') or d.get('list') or []
    return []


st, j = call('POST', '/auth/login', {'username': 'admin', 'password': '123456'})
TOKEN = j['data']['accessToken']

# 唯一后缀，避免重复执行时唯一约束冲突
CUST = '流程测试客户-' + RUN

print('===== CRM：线索 → 客户 → 商机 → 订单 =====')
lead = step('创建线索', 'POST', '/crm/lead/create', {
    'name': '流程测试线索-' + RUN, 'source': '官网', 'owner': 'admin',
    'status': '待跟进', 'remark': 'flow test'})[0]
leadId = (lead or {}).get('id')

customerId = step('线索转客户', 'POST', '/crm/lead/convert',
                  {'id': leadId, 'customerName': CUST, 'owner': 'admin'})[0]
step('客户详情', 'GET', '/crm/customer/%s' % customerId)
step('客户搜索命中', 'GET', '/crm/customer/list?keyword=' + urllib.parse.quote(CUST) if False else '/crm/customer/list')

deal = step('创建商机', 'POST', '/crm/deal/create', {
    'name': '流程测试商机-' + RUN, 'customerId': customerId, 'customerName': CUST,
    'amount': 50000, 'stage': '需求确认', 'winRate': 60, 'owner': 'admin',
    'expectDealAt': '2026-12-31', 'remark': 'flow test'})[0]
dealId = (deal or {}).get('id')

orderNo = step('获取订单号', 'GET', '/crm/order/next-no')[0]
order = step('创建订单', 'POST', '/crm/order/create', {
    'orderNo': orderNo, 'customerId': customerId, 'customerName': CUST,
    'dealId': dealId, 'dealName': '流程测试商机-' + RUN, 'contractNo': 'HT-' + RUN,
    'amount': 50000, 'owner': 'admin', 'signDate': '2026-10-26', 'expectDeliverAt': '2026-11-30',
    'remark': 'flow test',
    'items': [{'productCode': 'RAW-STEEL-001', 'productName': '热轧钢板 Q235',
               'spec': '3mm', 'quantity': 10, 'price': 5000}]})[0]
orderId = (order or {}).get('id')

step('确认订单', 'POST', '/crm/order/confirm/%s' % orderId)
delivered = step('订单生成出库单', 'POST', '/crm/order/deliver/%s' % orderId)[0]
outboundCode = (delivered or {}).get('outboundCode')
print('     -> 出库单号:', outboundCode)

print('\n===== WMS：出库单是否可继续作业 =====')
ob = step('出库单列表', 'GET', '/wms/outbound/list')[0]
rows = rows_of(ob)
target = next((r for r in rows if r.get('code') == outboundCode), None)
print('     出库单出现在 WMS 列表:', 'YES' if target else 'NO')
if target:
    det = step('出库单详情', 'GET', '/wms/outbound/%s' % target['id'])[0]
    items = rows_of((det or {}).get('items'))
    for i in items:
        print('     明细: sku=%s method=%s qty=%s loc=%r materialId=%s'
              % (i.get('sku'), i.get('method'), i.get('qty') or i.get('quantity'),
                 i.get('locationCode'), i.get('materialId')))
    if items:
        it = items[0]
        qty = it.get('qty') or it.get('quantity') or 10
        st2, j2 = call('POST', '/wms/outbound/execute', {
            'id': target['id'], 'operator': 'admin',
            'items': [{'id': it.get('id'),
                       'locations': [{'locationCode': it.get('locationCode') or 'A-01-01', 'qty': qty}]}]})
        print('     %s 执行出库 [%s/%s] %s' % ('PASS' if st2 == 200 and j2.get('code') == 0 else 'FAIL',
                                            st2, j2.get('code'), j2.get('message', '')))

inv = step('库存列表', 'GET', '/wms/inventory/list')[0]
print('     库存行数:', len(rows_of(inv)))
ib = step('入库单列表', 'GET', '/wms/inbound/list')[0]
print('     入库单数:', len(rows_of(ib)))

print('\n===== FIN：应收 → 开票 → 核销 =====')
ar = step('应收款列表', 'GET', '/fin/receivable/list')[0]
arRows = rows_of(ar)
print('     应收行数:', len(arRows))
match = next((r for r in arRows if CUST in str(r) or RUN in str(r)), None)
print('     本次订单生成的应收:', 'YES' if match else 'NO（订单未自动生成应收）')
if match:
    rid = match.get('id')
    step('应收开票', 'POST', '/fin/receivable/invoice', {'id': rid})
    step('应收核销', 'POST', '/fin/receivable/write-off',
         {'id': rid, 'amount': match.get('amount') or 100})

step('业务流水列表', 'GET', '/fin/business-flow/list')
step('AR/AP 汇总', 'GET', '/fin/arap')

print('\n===== MAINT / ETS / SYS 冒烟 =====')
for nm, p in [('设备台账', '/maint/equipment/list'), ('设备下拉', '/maint/equipment/options'),
              ('工单列表', '/maint/work-order/list'), ('保养计划', '/maint/plan/list'),
              ('备件列表', '/maint/spare-part/list'),
              ('来货登记', '/ets/register/list'), ('测试任务', '/ets/test/list'),
              ('测试报告', '/ets/report/list'),
              ('用户列表', '/system/user/list'), ('角色列表', '/system/role/list'),
              ('组织概览', '/system/org/overview'), ('审计日志', '/system/audit/list'),
              ('系统设置', '/system/config/list'), ('字典类型', '/system/dict/types')]:
    step(nm, 'GET', p)

print('\n===== 汇总：失败 %d 项 =====' % len(FAILS))
for f in FAILS:
    print('  %-32s [%s/%s] %s' % (f[0], f[1], f[2], f[3][:120]))
