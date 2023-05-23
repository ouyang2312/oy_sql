package com.oy.oy_sql.propertities;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 租户属性设置
 *
 * @author ouyang
 * @createDate 2023/5/22 11:32
 */
@ConfigurationProperties(prefix = "oy.sql.tenant")
public class TenantProperties {

    /** 是否开启租户id拼接 true开启，false关闭。 */
    private boolean open;
    /** 租户id字段 */
    private String tenantColumn;

    public String getTenantColumn() {
        return tenantColumn;
    }

    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
