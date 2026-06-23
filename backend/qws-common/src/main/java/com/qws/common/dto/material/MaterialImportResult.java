package com.qws.common.dto.material;

import java.util.List;

public class MaterialImportResult {
    private int successCount;
    private int failedCount;
    private List<String> failedSkus;
    private List<MaterialVO> items;

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public List<String> getFailedSkus() { return failedSkus; }
    public void setFailedSkus(List<String> failedSkus) { this.failedSkus = failedSkus; }

    public List<MaterialVO> getItems() { return items; }
    public void setItems(List<MaterialVO> items) { this.items = items; }
}
