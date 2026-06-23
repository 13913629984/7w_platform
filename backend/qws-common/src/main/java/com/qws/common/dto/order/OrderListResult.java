package com.qws.common.dto.order;

import java.util.List;
import java.util.Map;

public class OrderListResult {

    private List<OrderVO> rows;
    private long total;
    private int page;
    private int pageSize;
    private Map<String, Object> stats;
    private List<String> statuses;

    public List<OrderVO> getRows() { return rows; }
    public void setRows(List<OrderVO> rows) { this.rows = rows; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }

    public List<String> getStatuses() { return statuses; }
    public void setStatuses(List<String> statuses) { this.statuses = statuses; }
}
