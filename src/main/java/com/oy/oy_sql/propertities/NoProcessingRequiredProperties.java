package com.oy.oy_sql.propertities;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 不需处理sql的属性
 *
 * @author ouyang
 * @createDate 2023/5/23 17:14
 */
@ConfigurationProperties(prefix = "oy.sql.handle")
public class NoProcessingRequiredProperties {

    /** 不需要拼接租户id */
    private List<String> noTenantIds;
    /** 不需要逻辑删除 */
    private List<String> deleteIds;
    /** 没有租户id的表 */
    private List<String> noTenantTables;

    public List<String> getNoTenantIds() {
        return noTenantIds;
    }

    public void setNoTenantIds(List<String> noTenantIds) {
        this.noTenantIds = noTenantIds;
    }

    public List<String> getDeleteIds() {
        return deleteIds;
    }

    public void setDeleteIds(List<String> deleteIds) {
        this.deleteIds = deleteIds;
    }

    public List<String> getNoTenantTables() {
        return noTenantTables;
    }

    public void setNoTenantTables(List<String> noTenantTables) {
        this.noTenantTables = noTenantTables;
    }
}
