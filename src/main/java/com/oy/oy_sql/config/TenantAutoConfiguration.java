package com.oy.oy_sql.config;

import com.oy.oy_sql.core.SqlParseService;
import com.oy.oy_sql.core.TestService;
import com.oy.oy_sql.impl.ITenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 租户插件-自动装配
 *
 * @author ouyang
 * @createDate 2023/5/22 11:51
 */
@EnableConfigurationProperties(TenantProperties.class)
@Configuration
public class TenantAutoConfiguration {

    @Bean
    public TestService testService(){
        return new TestService();
    }

    @Autowired
    private TenantProperties tenantProperties;
    @Autowired
    private ITenantService tenantService;

    @Bean
    public SqlParseService sqlParseService(){
        SqlParseService sqlParseService = new SqlParseService();
        sqlParseService.setTenantProperties(tenantProperties);
        sqlParseService.setTenantService(tenantService);
        return sqlParseService;
    }

}
