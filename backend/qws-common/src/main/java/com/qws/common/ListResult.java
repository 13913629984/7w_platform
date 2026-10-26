package com.qws.common;

import java.util.List;

/**
 * 统一列表响应体。
 *
 * <p>平台上出现过三种列表返回结构：
 * <ul>
 *   <li>{@code {list, total, page, pageSize, stats}}（SYS 模块）；</li>
 *   <li>{@code {rows, total, page, pageSize, stats}}（CRM / MAINT / ETS / FIN）；</li>
 *   <li>裸数组（WMS 大部分列表）。</li>
 * </ul>
 * 裸数组的问题是拿不到总数，前端只能全量拉取再自己分页，数据量上来就崩。
 *
 * <p>这里给出统一形态。为了不破坏已上线的调用方，WMS 的"全部列表"接口
 * 改成返回本结构时同时保留数组兼容字段（见前端 {@code unwrapList} 助手）。
 *
 * @param rows     当前页数据
 * @param total    总条数（不分页的列表即 rows.size()）
 * @param page     当前页码，不分页时为 1
 * @param pageSize 每页条数，不分页时为 total
 * @param stats    可选统计，无则 null
 */
public record ListResult<T>(List<T> rows, long total, long page, long pageSize, Object stats) {

    /** 不分页的列表：total = 实际条数 */
    public static <T> ListResult<T> of(List<T> rows) {
        long size = rows == null ? 0 : rows.size();
        return new ListResult<>(rows, size, 1, size, null);
    }

    public static <T> ListResult<T> of(List<T> rows, Object stats) {
        long size = rows == null ? 0 : rows.size();
        return new ListResult<>(rows, size, 1, size, stats);
    }

    /** 分页列表 */
    public static <T> ListResult<T> page(List<T> rows, long total, long page, long pageSize) {
        return new ListResult<>(rows, total, page, pageSize, null);
    }
}
