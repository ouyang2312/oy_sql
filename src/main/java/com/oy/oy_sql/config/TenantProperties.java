package com.oy.oy_sql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 租户自动
 *
 * @author ouyang
 * @createDate 2023/5/22 11:32
 */
@ConfigurationProperties(prefix = "oy.sql")
public class TenantProperties {

    /** 租户id字段 */
    private String tenantColumn;
    /** 逻辑删除值 */
    private Integer deleteValue;
    /** 逻辑删除列 */
    private String deleteColumn;


    public String getTenantColumn() {
        return tenantColumn;
    }

    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }

    public Integer getDeleteValue() {
        return deleteValue;
    }

    public void setDeleteValue(Integer deleteValue) {
        this.deleteValue = deleteValue;
    }

    public String getDeleteColumn() {
        return deleteColumn;
    }

    public void setDeleteColumn(String deleteColumn) {
        this.deleteColumn = deleteColumn;
    }
}
