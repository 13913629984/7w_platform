package com.qws.common.dto.material;

import java.util.List;
import java.util.Map;

public class MaterialListResult {

    private List<MaterialVO> rows;
    private long total;
    private int page;
    private int pageSize;
    private Map<String, Object> stats;
    private List<Map<String, String>> categories;

    public List<MaterialVO> getRows() { return rows; }
    public void setRows(List<MaterialVO> rows) { this.rows = rows; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }

    public List<Map<String, String>> getCategories() { return categories; }
    public void setCategories(List<Map<String, String>> categories) { this.categories = categories; }
}
