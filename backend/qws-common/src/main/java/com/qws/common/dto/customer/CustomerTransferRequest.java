package com.qws.common.dto.customer;

import java.util.List;

public class CustomerTransferRequest {
    private List<Long> ids;
    private String owner;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
