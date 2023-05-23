package com.oy.oy_sql.core;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
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
        SQLExpr where = sqlStatement.getWhere();
        // 拼接条件
        SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr();
        if (sqlBinaryOpExpr != null) {
            sqlStatement.setWhere(sqlBinaryOpExpr);
            sqlStatement.addWhere(where);
        }
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

            // 拼接条件
            SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr();
            if (sqlBinaryOpExpr != null) {
                sqlUpdateStatement.setWhere(sqlBinaryOpExpr);
                sqlUpdateStatement.addWhere(where);
            }
            return SQLUtils.toSQLString(sqlUpdateStatement);
        } else {
            // 拼接条件
            SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr();
            if (sqlBinaryOpExpr != null) {
                sqlStatement.setWhere(sqlBinaryOpExpr);
                sqlStatement.addWhere(where);
            }
            return SQLUtils.toSQLString(sqlStatement);
        }
    }

    /***
     * hanlderSelect
     *
     * @param sqlStatement sqlStatement
     * @author ouyang
     * @date 2023/5/22 9:50
     */
    private String handleSelect(SQLSelectStatement sqlStatement) {
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
            // 拼接条件
            SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr();
            if (sqlBinaryOpExpr != null) {
                sqlSelectQueryBlock.setWhere(sqlBinaryOpExpr);
                sqlSelectQueryBlock.addWhere(where);
            }
        } else if (query instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) query;
            // 递归调用
            handleQuery(sqlUnionQuery.getLeft());
            handleQuery(sqlUnionQuery.getRight());
        }
    }

    /***
     * 准备拼接条件
     *
     * @return {@link SQLBinaryOpExpr}
     * @author ouyang
     * @date 2023/5/23 14:20
     */
    private SQLBinaryOpExpr prepareExpr() {
        // delete
        SQLBinaryOpExpr binaryOpExprDelete = null;
        if (logicDataProperties.isOpen()) {
            SQLIdentifierExpr column1ExprDelete = new SQLIdentifierExpr(logicDataProperties.getDeleteColumn());
            SQLNumericLiteralExpr value1ExprDelete = new SQLIntegerExpr(0);
            binaryOpExprDelete = new SQLBinaryOpExpr(column1ExprDelete, SQLBinaryOperator.Equality, value1ExprDelete);
        }

        // tenant_id
        SQLBinaryOpExpr binaryOpExprTenantId = null;
        if (tenantProperties.isOpen()) {
            SQLIdentifierExpr column1ExprTenantId = new SQLIdentifierExpr(tenantProperties.getTenantColumn());
            SQLNumericLiteralExpr value1ExprTenantId = new SQLIntegerExpr(1);
            binaryOpExprTenantId = new SQLBinaryOpExpr(column1ExprTenantId, SQLBinaryOperator.Equality, value1ExprTenantId);
        }

        // 构建多个 SQLExpr 的表达式
        if (binaryOpExprDelete != null && binaryOpExprTenantId != null) {
            SQLBinaryOpExpr finalExpr = new SQLBinaryOpExpr(binaryOpExprTenantId, SQLBinaryOperator.BooleanAnd, binaryOpExprDelete);
            return finalExpr;
        }
        // 不为空 则返回谁，如果都是空 则返回空
        return binaryOpExprTenantId != null ? binaryOpExprTenantId : binaryOpExprDelete;
    }

}
