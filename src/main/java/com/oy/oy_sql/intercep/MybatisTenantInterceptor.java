package com.oy.oy_sql.intercep;

import cn.hutool.core.util.ReflectUtil;
import com.oy.oy_sql.core.SqlParseService;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import java.lang.reflect.Field;
import java.sql.Connection;

/**
 * mybatis拦截器
 *
 * @author ouyang
 * @createDate 2023/5/23 11:46
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MybatisTenantInterceptor implements Interceptor {

    private SqlParseService sqlParseService;

    public void setSqlParseService(SqlParseService sqlParseService) {
        this.sqlParseService = sqlParseService;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取路由器
        RoutingStatementHandler statementHandler = (RoutingStatementHandler) invocation.getTarget();
        StatementHandler delegate = (StatementHandler) ReflectUtil.getFieldValue(statementHandler, "delegate");
        BoundSql boundSql = delegate.getBoundSql();
        MappedStatement mappedStatement = (MappedStatement) ReflectUtil.getFieldValue(delegate, "mappedStatement");

        // 获取方法路径
        String id = mappedStatement.getId();

        // 反射替换拼接sql
        Field field = boundSql.getClass().getDeclaredField("sql");
        field.setAccessible(true);
        try {
            String parser = sqlParseService.parse(boundSql.getSql(),id);
            // 调用验证分片字段
            field.set(boundSql, parser);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return invocation.proceed();
    }
}
