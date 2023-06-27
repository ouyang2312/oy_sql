package com.oy.oy_sql.config;

import com.oy.oy_sql.core.SqlParseService;
import com.oy.oy_sql.intercep.MybatisPlusTenantInterceptor;
import com.oy.oy_sql.intercep.MybatisTenantInterceptor;
import com.oy.oy_sql.propertities.LogicDataProperties;
import com.oy.oy_sql.propertities.NoProcessingRequiredProperties;
import com.oy.oy_sql.propertities.TenantProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 租户插件-自动装配
 *
 * @author ouyang
 * @createDate 2023/5/22 11:51
 */
@EnableConfigurationProperties({
        TenantProperties.class,
        LogicDataProperties.class,
        NoProcessingRequiredProperties.class
})
@Configuration
public class TenantAutoConfiguration {

    /***
     * 把解析服务加入ioc
     *
     * @return {@link SqlParseService}
     * @author ouyang
     * @date 2023/5/23 10:19
     */
    @Bean
    public SqlParseService sqlParseService(){
        SqlParseService sqlParseService = new SqlParseService();
        return sqlParseService;
    }

    /***
     * mybatis-plus拦截器
     *
     * @return {@link MybatisPlusTenantInterceptor}
     * @author ouyang
     * @date 2023/5/23 10:52
     */
    @Bean
    public MybatisPlusTenantInterceptor mybatisPlusTenantInterceptor(){
        MybatisPlusTenantInterceptor mybatisPlusTenantInterceptor = new MybatisPlusTenantInterceptor();
        return mybatisPlusTenantInterceptor;
    }

    /***
     * mybatis拦截器
     *
     * @return {@link MybatisTenantInterceptor}
     * @author ouyang
     * @date 2023/5/23 13:40
     */
    @Bean
    public MybatisTenantInterceptor mybatisTenantInterceptor(){
        MybatisTenantInterceptor mybatisTenantInterceptor = new MybatisTenantInterceptor();
        return mybatisTenantInterceptor;
    }

}
