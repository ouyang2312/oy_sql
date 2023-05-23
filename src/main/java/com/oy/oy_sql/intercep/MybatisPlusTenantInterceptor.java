package com.oy.oy_sql.intercep;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.oy.oy_sql.core.SqlParseService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 租户拦截器(mybatis-plus)
 *
 * @author ouyang
 * @createDate 2023/5/23 9:42
 */
public class MybatisPlusTenantInterceptor implements InnerInterceptor {

    private SqlParseService sqlParseService;

    public void setSqlParseService(SqlParseService sqlParseService) {
        this.sqlParseService = sqlParseService;
    }

    /***
     * 查询之前
     *
     * @param executor executor
     * @param ms ms
     * @param parameter parameter
     * @param rowBounds rowBounds
     * @param resultHandler resultHandler
     * @param boundSql boundSql
     * @author ouyang
     * @date 2023/5/23 11:28
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        /**
         *  1. 获取sql
         *  2. 修改sql
         *  3. 执行新sql
         */
        /** 1. 获取sql */
        String sql = boundSql.getSql();
        String parse = sqlParseService.parse(sql);

        // 修改sql
        Field field = null;
        try {
            field = boundSql.getClass().getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql,parse);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        /** 2. 修改sql */
        /** 3. 执行新sql */
        InnerInterceptor.super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
    }


    /***
     * 准备sql之前 （代码仿写 TenantLineInnerInterceptor ）
     *
     * @param sh sh
     * @param connection connection
     * @param transactionTimeout transactionTimeout
     * @author ouyang
     * @date 2023/5/23 11:24
     */
    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();

        if (sct == SqlCommandType.INSERT || sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE) {
            if (InterceptorIgnoreHelper.willIgnoreTenantLine(ms.getId())) {
                return;
            }

            PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
            String sql = mpBs.sql();

            String parse = sqlParseService.parse(sql);
            mpBs.sql(parse);
        }
    }

}
