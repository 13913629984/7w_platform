package com.qws.common.dto.customer;

import java.util.List;

public class CustomerImportRequest {
    private List<CustomerRequest> items;

    public List<CustomerRequest> getItems() { return items; }
    public void setItems(List<CustomerRequest> items) { this.items = items; }
}
