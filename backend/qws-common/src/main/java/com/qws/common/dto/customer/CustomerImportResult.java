package com.qws.common.dto.customer;

import java.util.List;

public class CustomerImportResult {
    private int successCount;
    private int failedCount;
    private List<String> failedNames;
    private List<CustomerVO> items;

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public List<String> getFailedNames() { return failedNames; }
    public void setFailedNames(List<String> failedNames) { this.failedNames = failedNames; }

    public List<CustomerVO> getItems() { return items; }
    public void setItems(List<CustomerVO> items) { this.items = items; }
}
