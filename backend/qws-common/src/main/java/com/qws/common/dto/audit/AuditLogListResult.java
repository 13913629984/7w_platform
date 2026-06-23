package com.qws.common.dto.audit;

import java.util.List;
import java.util.Map;

public class AuditLogListResult {
    private List<AuditLogVO> list;
    private long total;
    private int page;
    private int pageSize;
    private Map<String, Object> stats;

    public List<AuditLogVO> getList() { return list; }
    public void setList(List<AuditLogVO> list) { this.list = list; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }
}
