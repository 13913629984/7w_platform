package com.qws.common;

public record PageQuery(long pageNum, long pageSize, String keyword) {
    public long current() {
        return pageNum <= 0 ? 1 : pageNum;
    }

    public long size() {
        return pageSize <= 0 ? 10 : pageSize;
    }
}