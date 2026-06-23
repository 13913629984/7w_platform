package com.qws.common.dto.material;

import java.util.List;

public class MaterialImportRequest {
    private List<MaterialRequest> items;

    public List<MaterialRequest> getItems() { return items; }
    public void setItems(List<MaterialRequest> items) { this.items = items; }
}
