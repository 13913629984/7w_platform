package com.qws.common.dto.order;

import java.util.List;

public class OrderBatchDeleteRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
