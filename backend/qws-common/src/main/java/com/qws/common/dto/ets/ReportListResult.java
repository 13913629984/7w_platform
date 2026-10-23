package com.qws.common.dto.ets;

import java.util.List;
import java.util.Map;

/** 电测-测试报告列表返回 */
public class ReportListResult {

    private List<ReportVO> rows;
    private long total;
    private int page;
    private int pageSize;
    private Map<String, Object> stats;

    public List<ReportVO> getRows() { return rows; }
    public void setRows(List<ReportVO> rows) { this.rows = rows; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }
}
