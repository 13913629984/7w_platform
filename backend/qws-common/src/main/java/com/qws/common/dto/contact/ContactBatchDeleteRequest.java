package com.qws.common.dto.contact;

import java.util.List;

public class ContactBatchDeleteRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
