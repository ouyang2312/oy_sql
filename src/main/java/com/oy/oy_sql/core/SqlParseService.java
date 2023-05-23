package com.oy.oy_sql.core;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.oy.oy_sql.propertities.LogicDataProperties;
import com.oy.oy_sql.propertities.TenantProperties;
import com.oy.oy_sql.impl.ITenantService;

import java.util.List;

/**
 * sql解析
 *
 * @author ouyang
 * @createDate 2023/5/22 11:37
 */
public class SqlParseService {

    private ITenantService tenantService;
    private TenantProperties tenantProperties;
    private LogicDataProperties logicDataProperties;

    public void setTenantService(ITenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setTenantProperties(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    public void setLogicDataProperties(LogicDataProperties logicDataProperties) {
        this.logicDataProperties = logicDataProperties;
    }

    /***
     * parse
     *
     * @param sql sql
     * @return {@link String}
     * @author ouyang
     * @date 2023/5/22 11:40
     */
    public String parse(String sql) {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        StringBuffer sb = new StringBuffer();
        for (SQLStatement sqlStatement : sqlStatements) {
            // 解析当前的语句是什么类型
            if (sqlStatement instanceof SQLDeleteStatement) {
                sb.append(handleDelete((SQLDeleteStatement) sqlStatement));
            } else if (sqlStatement instanceof SQLInsertStatement) {
                sb.append(handleInsert((SQLInsertStatement) sqlStatement));
            } else if (sqlStatement instanceof SQLUpdateStatement) {
                sb.append(handleUpdate((SQLUpdateStatement) sqlStatement));
            } else if (sqlStatement instanceof SQLSelectStatement) {
                sb.append(handleSelect((SQLSelectStatement) sqlStatement));
            }
        }
        return sb.toString();
    }


    /***
     * handleUpdate
     *
     * @param sqlStatement sqlStatement
     * @author ouyang
     * @date 2023/5/22 11:16
     */
    private String handleUpdate(SQLUpdateStatement sqlStatement) {
        if (!tenantProperties.isOpen()) {
            return SQLUtils.toSQLString(sqlStatement);
        }

        SQLExpr where = sqlStatement.getWhere();
        SQLExpr expr = SQLUtils.toSQLExpr(tenantProperties.getTenantColumn() + " = " + tenantService.getTenantId(), JdbcConstants.MYSQL);

        SQLBinaryOpExpr sqlBinaryOpExpr = new SQLBinaryOpExpr(expr, where, SQLBinaryOperator.BooleanAnd);
        sqlStatement.setWhere(sqlBinaryOpExpr);

        return SQLUtils.toSQLString(sqlStatement);
    }

    /**
     * handleInsert
     *
     * @param sqlStatement sqlStatement
     * @author ouyang
     * @date 2023/5/22 11:09
     */
    private String handleInsert(SQLInsertStatement sqlStatement) {
        if (!tenantProperties.isOpen()) {
            return SQLUtils.toSQLString(sqlStatement);
        }

        sqlStatement.addColumn(SQLUtils.toSQLExpr(tenantProperties.getTenantColumn()));
        SQLInsertStatement.ValuesClause values = sqlStatement.getValues();
        values.addValue(SQLUtils.toSQLExpr(tenantService.getTenantId()));

        return SQLUtils.toSQLString(sqlStatement);
    }

    /***
     * hanlderDelete
     *
     * @param sqlStatement sqlStatement
     * @author ouyang
     * @date 2023/5/22 10:19
     * @return
     */
    private String handleDelete(SQLDeleteStatement sqlStatement) {
        // 下面注释代码是讲2个条件组合
        // 逻辑删除
        SQLExpr where = sqlStatement.getWhere();

        // 是否开启了逻辑删除 or 是否加了不用逻辑删除注解
        if (logicDataProperties.isOpen()) {
            // 准备update
            SQLUpdateStatement sqlUpdateStatement = new SQLUpdateStatement();
            sqlUpdateStatement.setTableSource(sqlStatement.getTableSource());
            // set 内容
            SQLUpdateSetItem sqlUpdateSetItem = new SQLUpdateSetItem();
            sqlUpdateSetItem.setColumn(SQLUtils.toSQLExpr(logicDataProperties.getDeleteColumn(), JdbcConstants.MYSQL));
            sqlUpdateSetItem.setValue(new SQLIntegerExpr(1));
            sqlUpdateStatement.addItem(sqlUpdateSetItem);

            // 条件
            if (tenantProperties.isOpen()) {
                SQLExpr expr = SQLUtils.toSQLExpr(tenantProperties.getTenantColumn() + " = " + tenantService.getTenantId(), JdbcConstants.MYSQL);
                SQLBinaryOpExpr sqlBinaryOpExpr = new SQLBinaryOpExpr(expr, where, SQLBinaryOperator.BooleanAnd);
                sqlUpdateStatement.setWhere(sqlBinaryOpExpr);
            } else {
                sqlUpdateStatement.setWhere(where);
            }
            return SQLUtils.toSQLString(sqlUpdateStatement);
        } else {
            if (tenantProperties.isOpen()) {
                // 构造
                SQLExpr logicalCondition = SQLUtils.toSQLExpr(tenantProperties.getTenantColumn() + " = " + tenantService.getTenantId());
                // 获取原始删除条件
                SQLExpr originalCondition = sqlStatement.getWhere();

                // 将原始删除条件与逻辑删除条件进行组合
                SQLBinaryOpExpr combinedCondition = new SQLBinaryOpExpr(originalCondition, logicalCondition, SQLBinaryOperator.BooleanAnd);
                sqlStatement.setWhere(combinedCondition);
                return SQLUtils.toSQLString(sqlStatement);
            }
        }

        // 返回正常的
        return SQLUtils.toSQLString(sqlStatement);
    }

    /***
     * hanlderSelect
     *
     * @param sqlStatement sqlStatement
     * @author ouyang
     * @date 2023/5/22 9:50
     */
    private String handleSelect(SQLSelectStatement sqlStatement) {
        if (!tenantProperties.isOpen()) {
            return SQLUtils.toSQLString(sqlStatement);
        }
        SQLSelect select = sqlStatement.getSelect();
        SQLSelectQuery query = select.getQuery();

        handleQuery(query);
        return SQLUtils.toSQLString(query);
    }

    /***
     * 处理查询
     *
     * @param query query
     * @author ouyang
     * @date 2023/5/23 9:25
     */
    private void handleQuery(SQLSelectQuery query) {
        // 区分类型  SQLSelectQueryBlock 和 SQLUnionQuery
        if (query instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) query;
            SQLExpr where = sqlSelectQueryBlock.getWhere();
            sqlSelectQueryBlock.setWhere(
                    SQLUtils.toSQLExpr(tenantProperties.getTenantColumn() + " = " + tenantService.getTenantId(), DbType.mysql)
            );
            sqlSelectQueryBlock.addWhere(where);
        } else if (query instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) query;
            // 递归调用
            handleQuery(sqlUnionQuery.getLeft());
            handleQuery(sqlUnionQuery.getRight());
        }
    }

}
