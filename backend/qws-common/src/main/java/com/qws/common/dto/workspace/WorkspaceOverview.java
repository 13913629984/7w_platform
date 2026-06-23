package com.qws.common.dto.workspace;

import java.util.List;
import java.util.Map;

public class WorkspaceOverview {

    private Map<String, Object> stats;
    private List<WorkspaceLeadVO> leads;
    private List<WorkspaceActivityVO> activities;
    private List<Map<String, Object>> salesTrend;
    private List<Map<String, Object>> customerDistribution;

    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }

    public List<WorkspaceLeadVO> getLeads() { return leads; }
    public void setLeads(List<WorkspaceLeadVO> leads) { this.leads = leads; }

    public List<WorkspaceActivityVO> getActivities() { return activities; }
    public void setActivities(List<WorkspaceActivityVO> activities) { this.activities = activities; }

    public List<Map<String, Object>> getSalesTrend() { return salesTrend; }
    public void setSalesTrend(List<Map<String, Object>> salesTrend) { this.salesTrend = salesTrend; }

    public List<Map<String, Object>> getCustomerDistribution() { return customerDistribution; }
    public void setCustomerDistribution(List<Map<String, Object>> customerDistribution) { this.customerDistribution = customerDistribution; }
}
