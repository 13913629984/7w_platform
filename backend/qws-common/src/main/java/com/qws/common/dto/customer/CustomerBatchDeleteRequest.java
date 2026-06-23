package com.qws.common.dto.customer;

import java.util.List;

public class CustomerBatchDeleteRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
