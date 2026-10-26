#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""清理第 1 批回归产生的测试数据（只删本轮标记）"""
import json, urllib.request, urllib.error

BASE='http://localhost:8080/api'; TOKEN=None

def call(m,p,b=None):
    r=urllib.request.Request(BASE+p, data=json.dumps(b,ensure_ascii=False).encode() if b is not None else None, method=m)
    r.add_header('Content-Type','application/json; charset=utf-8')
    if TOKEN: r.add_header('Authorization','Bearer '+TOKEN)
    try:
        with urllib.request.urlopen(r,timeout=30) as x: return json.loads(x.read())
    except urllib.error.HTTPError as e:
        try: return json.loads(e.read())
        except: return {}

def rows(p):
    d=call('GET',p).get('data')
    if isinstance(d,list): return d
    if isinstance(d,dict): return d.get('rows') or d.get('list') or []
    return []

TOKEN=call('POST','/auth/login',{'username':'admin','password':'123456'})['data']['accessToken']
MARK=('流程测试','FLOW','UI测试','HT-UI-','HT-CLOSE-','HT-NOMAT-','HT-SHORT-','HT-UNIT-','SO20261026','PO20261026','OUT2026100','IN-PO20261026')

deleted={}
def del_list(label, lp, dp, idk='id', skip=()):
    n=0
    for r in rows(lp):
        s=json.dumps(r,ensure_ascii=False)
        if not any(k in s for k in MARK): continue
        if any(k in s for k in skip): continue
        rid=r.get(idk)
        if rid is None: continue
        call('POST',dp,{'id':rid}); n+=1
    deleted[label]=n

del_list('出库单','/wms/outbound/list','/wms/outbound/delete')
del_list('入库单','/wms/inbound/list','/wms/inbound/delete')
del_list('采购订单','/wms/purchase-order/list','/wms/purchase-order/delete')
del_list('CRM订单','/crm/order/list','/crm/order/delete')
del_list('商机','/crm/deal/list','/crm/deal/delete')
del_list('线索','/crm/lead/list','/crm/lead/delete')
del_list('工单','/maint/work-order/list','/maint/work-order/delete')
del_list('计划','/maint/plan/list','/maint/plan/delete')
del_list('设备','/maint/equipment/list','/maint/equipment/delete')
del_list('报告','/ets/report/list','/ets/report/delete')
del_list('测试','/ets/test/list','/ets/test/delete')
del_list('来货登记','/ets/register/list','/ets/register/delete')
del_list('客户','/crm/customer/list','/crm/customer/delete')
print(json.dumps(deleted,ensure_ascii=False,indent=1))

# 库存归零
inv=rows('/wms/inventory/list')
for x in inv:
    if x.get('quantity'):
        call('POST','/wms/inventory/update',{'id':x['id'],'materialId':x['materialId'],'warehouseCode':x['warehouseCode'],
            'locationCode':x['locationCode'],'quantity':0,'availableQty':0,'lockedQty':0,
            'batchNo':x.get('batchNo') or '','safeStock':x.get('safeStock') or 0,'status':1})
print('库存:', [(x['warehouseCode'],x['locationCode'],x['quantity']) for x in rows('/wms/inventory/list')])
