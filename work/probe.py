import json,sys
sys.path.insert(0,'work')
exec(open('work/api_smoke.py').read().split('idpool')[0].split('# 先跑')[0])
st,j=call('POST','/auth/login',{'username':'admin','password':'123456'},token=None) if False else (0,None)
