package com.oy.oy_sql.impl;

/**
 * 租户实现（给业务端实现）
 *
 * @author ouyang
 * @createDate 2023/5/22 11:45
 */
public interface ITenantService {

    /** 获取租户id */
    String getTenantId();

}
