package com.qws.common.audit;

/**
 * 审计操作类型。取值与前端审计页的筛选下拉一一对应。
 */
public enum ActionType {

    LOGIN("登录"),
    LOGOUT("登出"),
    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    QUERY("查询"),
    EXPORT("导出"),
    IMPORT("导入"),
    APPROVE("审批"),
    EXECUTE("执行"),
    STATUS("状态变更");

    private final String label;

    ActionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
