package com.oy.oy_sql.propertities;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 逻辑删除数据 属性
 *
 * @author ouyang
 * @createDate 2023/5/23 10:09
 */
@ConfigurationProperties(prefix = "oy.sql.delete")
public class LogicDataProperties {

    /** 是否开启逻辑删除 true开启*/
    private boolean open;
    /** 逻辑删除值 */
    private Integer deleteValue;
    /** 逻辑删除列 */
    private String deleteColumn;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
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
