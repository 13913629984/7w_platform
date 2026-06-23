package com.qws.common.dto.material;

import java.util.List;

public class MaterialBatchDeleteRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
