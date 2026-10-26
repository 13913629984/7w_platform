#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""全接口冒烟：登录 -> 遍历所有 GET 接口(带 id 自动取真实 id) -> 汇总失败"""
import json, re, glob, sys, urllib.request, urllib.error

BASE = 'http://localhost:8080/api'

def call(method, path, body=None, token=None, timeout=20):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode('utf-8', 'replace')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'replace')
    except Exception as e:
        return -1, repr(e)

st, body = call('POST', '/auth/login', {'username': 'admin', 'password': '123456'})
token = json.loads(body)['data']['accessToken']

# 收集后端 GET 端点
eps = []
for f in glob.glob('backend/qws-*/src/main/java/**/*Controller.java', recursive=True):
    src = open(f, encoding='utf-8').read()
    m = re.search(r'@RequestMapping\("([^"]*)"\)', src)
    base = m.group(1) if m else ''
    for mm in re.finditer(r'@(Get|Post)Mapping(?:\("([^"]*)"\)|\(\))', src):
        if mm.group(1) != 'Get':
            continue
        p = (base + (mm.group(2) or '')).replace('//', '/')
        eps.append((p or '/', f.replace(chr(92), '/').split('qws-')[-1].split('/')[0]))

# 先跑 list 类接口，抓 id 池
idpool = {}
order = sorted(eps, key=lambda x: ('{' in x[0], x[0]))
results = []
for path, svc in order:
    real = path
    if '{' in path:
        prefix = path.split('{')[0].rstrip('/')
        cand = idpool.get(prefix)
        if cand is None:
            # 尝试该资源的 list 接口
            for guess in (prefix + '/list', prefix + 's', prefix):
                st2, b2 = call('GET', guess, token=token)
                if st2 == 200:
                    try:
                        d = json.loads(b2)['data']
                        rows = d.get('rows') or d.get('list') or (d if isinstance(d, list) else [])
                        if rows and isinstance(rows[0], dict) and rows[0].get('id'):
                            cand = rows[0]['id']
                            break
                    except Exception:
                        pass
        real = re.sub(r'\{[^}]+\}', str(cand if cand else 1), path)
    full = real
    st, b = call('GET', full, token=token)
    ok = st == 200 and '"code":0' in b.replace(' ', '')
    note = ''
    if not ok:
        try:
            j = json.loads(b)
            note = str(j.get('message') or j.get('error') or '')[:80]
        except Exception:
            note = b[:80]
    results.append((st, path, real, svc, ok, note))
    if st == 200:
        try:
            d = json.loads(b)['data']
            rows = d.get('rows') or d.get('list') if isinstance(d, dict) else d
            if isinstance(rows, list) and rows and isinstance(rows[0], dict) and rows[0].get('id'):
                idpool.setdefault(path.split('{')[0].rstrip('/'), rows[0]['id'])
        except Exception:
            pass

bad = [r for r in results if not r[4]]
print('GET 接口总数: %d, 失败: %d\n' % (len(results), len(bad)))
for st, path, real, svc, ok, note in bad:
    print('[%s] %-4s %-42s -> %-46s %s' % (svc, st, path, real, note))
