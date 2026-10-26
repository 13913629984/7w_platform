#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""写入链路测试：采购需求→采购订单→入库→库存→出库→CRM订单出库；维保/电测/财务/SYS 新增删除"""
import json, time, urllib.request, urllib.error

BASE = 'http://localhost:8080/api'
TOKEN = None
RUN = time.strftime('%H%M%S')
N = [0]
FAILS = []


def call(method, path, body=None, timeout=40):
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
            return e.code, {'raw': raw[:300]}
    except Exception as e:
        return -1, {'raw': repr(e)}


def step(name, method, path, body=None):
    N[0] += 1
    st, j = call(method, path, body)
    code = j.get('code') if isinstance(j, dict) else None
    ok = st == 200 and code == 0
    msg = (j.get('message') if isinstance(j, dict) else '') or ''
    if not ok:
        FAILS.append((name, st, code, msg))
    print('%s %02d. %-34s [%s/%s] %s' % ('PASS' if ok else 'FAIL', N[0], name, st, code,
                                         '' if ok else msg[:180]))
    return (j.get('data') if ok and isinstance(j, dict) else None), ok, msg


def rows_of(d):
    if isinstance(d, list):
        return d
    if isinstance(d, dict):
        return d.get('rows') or d.get('list') or []
    return []


st, j = call('POST', '/auth/login', {'username': 'admin', 'password': '123456'})
TOKEN = j['data']['accessToken']

print('===== A. 采购链：需求 → 采购订单 → 入库单 → 执行入库 → 库存 =====')
mat = rows_of(step('物料列表', 'GET', '/wms/material/list')[0])
material = mat[0] if mat else None
mid = material['id']
outs = rows_of(step('仓库列表', 'GET', '/wms/warehouse/list')[0])
wh = next((w for w in outs if w.get('status') == 1), outs[0]) if outs else None
locs = rows_of(step('库位列表', 'GET', '/wms/location/list')[0])
loc = next((l for l in locs if l.get('warehouseCode') == wh['code']), None) or (locs[0] if locs else None)
print('     用物料=%s 仓库=%s 库位=%s' % (material.get('sku'), wh.get('code'), loc.get('code')))

po = step('创建采购订单', 'POST', '/wms/purchase-order/create', {
    'supplier': '流程测试供应商', 'warehouseCode': wh['code'], 'demandCodes': [],
    'deliveryDate': '2026-12-01', 'creator': 'admin', 'remark': 'flow test',
    'items': [{'materialId': mid, 'sku': material['sku'], 'name': material['name'],
               'spec': material.get('spec'), 'qty': 20, 'unitPrice': 850}]})[0]
poId = (po or {}).get('id')
print('     采购订单:', (po or {}).get('code'), 'status=', (po or {}).get('status'))

step('确认采购订单', 'POST', '/wms/purchase-order/confirm', {'id': poId})
created = step('采购订单生成入库单', 'POST', '/wms/purchase-order/create-inbound', {'id': poId})[0]
print('     入库单号:', (created or {}).get('inboundCode'), 'status=', (created or {}).get('status'))

ib = rows_of(step('入库单列表', 'GET', '/wms/inbound/list')[0])
ibTarget = next((r for r in ib if (created or {}).get('inboundCode') == r.get('code')), None)
invBefore = rows_of(step('库存快照(前)', 'GET', '/wms/inventory/list')[0])
qtyBefore = sum(float(r.get('quantity') or 0) for r in invBefore)
if ibTarget:
    det = step('入库单详情', 'GET', '/wms/inbound/%s' % ibTarget['id'])[0]
    items = rows_of((det or {}).get('items'))
    payload = {'id': ibTarget['id'], 'operator': 'admin', 'items': [
        {'id': it['id'], 'locations': [{'locationCode': loc['code'], 'qty': it.get('qty')}]} for it in items]}
    step('执行入库', 'POST', '/wms/inbound/execute', payload)
    invAfter = rows_of(step('库存快照(后)', 'GET', '/wms/inventory/list')[0])
    qtyAfter = sum(float(r.get('quantity') or 0) for r in invAfter)
    print('     库存总量 %s -> %s (变化 %+g)' % (qtyBefore, qtyAfter, qtyAfter - qtyBefore))
    print('     库存明细:', [(r.get('sku'), r.get('warehouseCode'), r.get('locationCode'),
                             r.get('quantity'), r.get('availableQty')) for r in invAfter])

print('\n===== B. 出库链：CRM订单 → 出库单 → 执行出库 → 库存 =====')
cust = rows_of(step('客户列表', 'GET', '/crm/customer/list')[0])
c = cust[0] if cust else None
orderNo = step('获取订单号', 'GET', '/crm/order/next-no')[0]
order = step('创建订单', 'POST', '/crm/order/create', {
    'orderNo': orderNo, 'customerId': c['id'], 'customerName': c['name'],
    'contractNo': 'HT-OUT-' + RUN, 'amount': 17000, 'owner': 'admin',
    'signDate': '2026-10-26', 'expectDeliverAt': '2026-11-30', 'remark': 'flow test',
    'items': [{'productCode': material['sku'], 'productName': material['name'],
               'spec': material.get('spec'), 'quantity': 5, 'price': 3400}]})[0]
oid = (order or {}).get('id')
step('确认订单', 'POST', '/crm/order/confirm/%s' % oid)
delivered = step('订单生成出库单', 'POST', '/crm/order/deliver/%s' % oid)[0]
obCode = (delivered or {}).get('outboundCode')
print('     出库单号:', obCode)
ob = rows_of(step('出库单列表', 'GET', '/wms/outbound/list')[0])
obTarget = next((r for r in ob if r.get('code') == obCode), None)
print('     出库单仓库:', (obTarget or {}).get('warehouseCode'))
invBefore2 = rows_of(call('GET', '/wms/inventory/list')[1].get('data')) or []
if obTarget:
    store = (obTarget or {}).get('warehouseCode')
    loc2 = next((l for l in locs if l.get('warehouseCode') == store), None)
    print('     出库单仓库=%s, 可选库位=%s' % (store, loc2 and loc2.get('code')))
    det = step('出库单详情', 'GET', '/wms/outbound/%s' % obTarget['id'])[0]
    items = rows_of((det or {}).get('items'))
    it = items[0] if items else None
    payload = {'id': obTarget['id'], 'operator': 'admin', 'items': [
        {'id': it['id'], 'locations': [{'locationCode': (loc2 or {}).get('code') or it.get('locationCode'),
                                        'qty': it.get('qty')}]}]}
    r = step('执行出库', 'POST', '/wms/outbound/execute', payload)
    invAfter2 = rows_of(call('GET', '/wms/inventory/list')[1].get('data')) or []
    qA = sum(float(x.get('quantity') or 0) for x in invAfter2)
    qB = sum(float(x.get('quantity') or 0) for x in invBefore2)
    print('     库存总量 %s -> %s (变化 %+g)' % (qB, qA, qA - qB))

print('\n===== C. 维保：设备 → 工单 → 状态流转 / 保养计划执行 =====')
eq = step('创建设备', 'POST', '/maint/equipment/create', {
    'code': 'EQ-FLOW-' + RUN, 'name': '流程测试设备', 'category': '电气', 'model': 'M-' + RUN,
    'location': '车间A', 'status': '运行', 'owner': 'admin', 'purchaseDate': '2026-01-01'})[0]
eqId = (eq or {}).get('id')
wo = step('创建工单', 'POST', '/maint/work-order/create', {
    'title': '流程测试工单-' + RUN, 'equipmentId': eqId,
    'type': 'repair', 'priority': 'high', 'status': 'pending', 'handler': 'admin',
    'faultDesc': 'flow test', 'reportTime': '2026-10-26T10:00:00', 'reporter': 'admin'})[0]
woId = (wo or {}).get('id')
if woId:
    # 状态机是 pending -> assigned -> processing -> completed -> closed，不能跳级
    step('工单派单', 'POST', '/maint/work-order/status', {'id': woId, 'status': 'assigned', 'handler': 'admin'})
    step('工单开始处理', 'POST', '/maint/work-order/status', {'id': woId, 'status': 'processing'})
    step('工单完成', 'POST', '/maint/work-order/status', {'id': woId, 'status': 'completed'})
sp = step('备件', 'GET', '/maint/spare-part/list')[0]
pl = step('创建保养计划', 'POST', '/maint/plan/create', {
    'name': '流程测试保养计划-' + RUN, 'equipmentId': eqId, 'type': 'monthly',
    'cycleDays': 30, 'nextDate': '2026-11-26', 'status': 'pending', 'handler': 'admin'})[0]
plId = (pl or {}).get('id')
if plId:
    step('执行保养计划', 'POST', '/maint/plan/execute', {'id': plId})

print('\n===== D. 电测：来货登记 → 测试任务 → 结果 → 报告 =====')
reg = step('创建来货登记', 'POST', '/ets/register/create', {
    'customerName': '流程测试客户', 'product': '电源模块',
    'model': 'PM-' + RUN, 'quantity': 3, 'sampleQty': 1, 'arriveDate': '2026-10-26',
    'status': 'pending', 'operator': 'admin', 'remark': 'flow test'})[0]
regId = (reg or {}).get('id')
print('     登记:', (reg or {}).get('code'))
tt = step('创建测试任务', 'POST', '/ets/test/create', {
    'registerId': regId, 'testItem': '耐压测试', 'category': 'withstand',
    'standardValue': '1500V/60s', 'measuredValue': '1500V/60s 无击穿', 'result': 'pending',
    'tester': 'admin', 'testTime': '2026-10-26T11:00:00'})[0]
ttId = (tt or {}).get('id')
if ttId:
    step('录入测试结果', 'POST', '/ets/test/result', {'id': ttId, 'result': 'pass', 'measuredValue': '通过'})
rp = step('创建测试报告', 'POST', '/ets/report/create', {
    'registerId': regId, 'customerName': '流程测试客户', 'product': '电源模块',
    'model': 'PM-' + RUN, 'quantity': 3, 'sampleQty': 1, 'passQty': 1, 'failQty': 0,
    'conclusion': '合格', 'inspector': 'admin', 'status': 'pending'})[0]
rpId = (rp or {}).get('id')
if rpId:
    step('报告审核', 'POST', '/ets/report/approve', {'id': rpId, 'action': 'approve', 'approver': 'admin'})
    step('报告签发', 'POST', '/ets/report/issue', {'id': rpId})

print('\n===== E. 财务：应收 → 开票 → 核销 / 应付 → 匹配 / 费用 → 审批 =====')
ar = step('手工创建应收', 'POST', '/fin/receivable/create', {
    'code': 'AR-FLOW-' + RUN, 'customer': '流程测试客户', 'amount': 12345,
    'dueDate': '2026-12-31', 'status': '待收款', 'remark': 'flow test'})[0]
print('     应收返回:', json.dumps(ar, ensure_ascii=False)[:200] if ar else None)
if ar and ar.get('id'):
    step('应收开票', 'POST', '/fin/receivable/invoice', {'id': ar['id']})
    step('应收核销', 'POST', '/fin/receivable/write-off', {'id': ar['id'], 'amount': 12345})

ap = step('创建应付', 'POST', '/fin/payable/create', {
    'supplier': '流程测试供应商', 'purchaseOrder': 'PO-FLOW-' + RUN,
    'purchaseAmount': 8000, 'tax': 1040, 'dueDate': '2026-12-31', 'status': 'pending'})[0]
print('     应付返回:', json.dumps(ap, ensure_ascii=False)[:200] if ap else None)

ex = step('创建费用', 'POST', '/fin/expense/create', {
    'type': 'sales', 'module': 'CRM', 'item': '差旅费', 'amount': 500,
    'date': '2026-10-26', 'applicant': 'admin'})[0]
if ex and ex.get('id'):
    step('费用审批', 'POST', '/fin/expense/approve', {'id': ex['id']})

print('\n===== F. SYS：用户/角色/组织/字典 增删 =====')
u = step('创建用户', 'POST', '/system/user/create', {
    'username': 'flow' + RUN, 'nickname': '流程测试', 'password': '123456',
    'email': 'f@x.com', 'phone': '13900000000', 'status': '启用', 'role': 'USER'})[0]
uid = (u or {}).get('id')
if uid:
    step('更新用户', 'POST', '/system/user/update', {'id': uid, 'nickname': '流程测试2'})
    step('删除用户', 'POST', '/system/user/delete', {'id': uid})
r = step('创建角色', 'POST', '/system/role/create', {
    'code': 'FLOW' + RUN, 'name': '流程测试角色', 'description': 'flow test'})[0]
rid = (r or {}).get('id')
if rid:
    step('角色授权', 'POST', '/system/role/permissions',
         {'roleCode': 'FLOW' + RUN, 'permissionCodes': ['CRM', 'WMS']})
    step('删除角色', 'POST', '/system/role/delete', {'id': rid})
d = step('创建部门', 'POST', '/system/org/department/create', {
    'name': '流程测试部门' + RUN, 'parentId': None, 'leader': 'admin', 'status': 1})[0]
if d and d.get('id'):
    step('删除部门', 'POST', '/system/org/department/delete', {'id': d['id']})
dt = step('创建字典类型', 'POST', '/system/dict/type/create',
          {'dictCode': 'flow_' + RUN, 'dictName': '流程测试字典', 'description': 'flow'})[0]
if dt and dt.get('id'):
    step('删除字典类型', 'POST', '/system/dict/type/delete', {'id': dt['id']})

print('\n===== 汇总：失败 %d 项 =====' % len(FAILS))
for f in FAILS:
    print('  %-34s [%s/%s] %s' % (f[0], f[1], f[2], f[3][:160]))
