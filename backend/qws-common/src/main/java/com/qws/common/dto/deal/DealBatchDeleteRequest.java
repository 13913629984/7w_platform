package com.qws.common.dto.deal;

import java.util.List;

public class DealBatchDeleteRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
