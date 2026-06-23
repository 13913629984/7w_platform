package com.qws.common.dto.deal;

import java.util.List;
import java.util.Map;

public class DealListResult {

    private List<DealVO> rows;
    private long total;
    private int page;
    private int pageSize;
    private Map<String, Object> stats;
    private List<String> stages;

    public List<DealVO> getRows() { return rows; }
    public void setRows(List<DealVO> rows) { this.rows = rows; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }

    public List<String> getStages() { return stages; }
    public void setStages(List<String> stages) { this.stages = stages; }
}
