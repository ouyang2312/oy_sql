package com.oy.oy_sql.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.oy.oy_sql.propertities.LogicDataProperties;
import com.oy.oy_sql.propertities.NoProcessingRequiredProperties;
import com.oy.oy_sql.propertities.TenantProperties;
import com.oy.oy_sql.impl.ITenantService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * sql解析
 *
 * @author ouyang
 * @createDate 2023/5/22 11:37
 */
public class SqlParseService {

    @Autowired
    private ITenantService tenantService;
    @Autowired
    private TenantProperties tenantProperties;
    @Autowired
    private LogicDataProperties logicDataProperties;
    @Autowired
    private NoProcessingRequiredProperties noProcessingRequiredProperties;

    /***
     * parse
     *
     * @param sql sql
     * @param mapperId
     * @return {@link String}
     * @author ouyang
     * @date 2023/5/22 11:40
     */
    public String parse(String sql, String mapperId) {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        StringBuffer sb = new StringBuffer();
        for (SQLStatement sqlStatement : sqlStatements) {
            // 解析当前的语句是什么类型
            if (sqlStatement instanceof SQLDeleteStatement) {
                sb.append(handleDelete((SQLDeleteStatement) sqlStatement, mapperId));
            } else if (sqlStatement instanceof SQLInsertStatement) {
                sb.append(handleInsert((SQLInsertStatement) sqlStatement));
            } else if (sqlStatement instanceof SQLUpdateStatement) {
                sb.append(handleUpdate((SQLUpdateStatement) sqlStatement, mapperId));
            } else if (sqlStatement instanceof SQLSelectStatement) {
                sb.append(handleSelect((SQLSelectStatement) sqlStatement, mapperId));
            }
        }
        return sb.toString();
    }


    /***
     * handleUpdate
     *
     * @param sqlStatement sqlStatement
     * @param mapperId
     * @author ouyang
     * @date 2023/5/22 11:16
     */
    private String handleUpdate(SQLUpdateStatement sqlStatement, String mapperId) {
        SQLExpr where = sqlStatement.getWhere();
        // 拼接条件
        String simpleName = sqlStatement.getTableName().getSimpleName();
        SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr(mapperId, simpleName,false);
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
        String simpleName = sqlStatement.getTableName().getSimpleName();
        if (!tenantProperties.isOpen() || noTenantTableFlag(simpleName)) {
            return SQLUtils.toSQLString(sqlStatement);
        }

        sqlStatement.addColumn(SQLUtils.toSQLExpr(tenantProperties.getTenantColumn()));
        SQLInsertStatement.ValuesClause values = sqlStatement.getValues();
        values.addValue(SQLUtils.toSQLExpr(String.valueOf(tenantService.getTenantId())));

        return SQLUtils.toSQLString(sqlStatement);
    }

    /***
     * hanlderDelete
     *
     * @param sqlStatement sqlStatement
     * @param mapperId
     * @author ouyang
     * @date 2023/5/22 10:19
     * @return
     */
    private String handleDelete(SQLDeleteStatement sqlStatement, String mapperId) {
        // 下面注释代码是讲2个条件组合
        // 逻辑删除
        SQLExpr where = sqlStatement.getWhere();
        String simpleName = sqlStatement.getTableName().getSimpleName();

        // 是否开启了逻辑删除 or 是否加了不用逻辑删除注解
        if (logicDataProperties.isOpen() && !deleteFlag(mapperId)) {
            // 准备update
            SQLUpdateStatement sqlUpdateStatement = new SQLUpdateStatement();
            sqlUpdateStatement.setTableSource(sqlStatement.getTableSource());
            // set 内容
            SQLUpdateSetItem sqlUpdateSetItem = new SQLUpdateSetItem();
            sqlUpdateSetItem.setColumn(SQLUtils.toSQLExpr(logicDataProperties.getDeleteColumn(), JdbcConstants.MYSQL));
            sqlUpdateSetItem.setValue(new SQLNumberExpr(Long.valueOf(logicDataProperties.getDeleteValue())));
            sqlUpdateStatement.addItem(sqlUpdateSetItem);

            // 拼接条件
            SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr(mapperId, simpleName, false);
            if (sqlBinaryOpExpr != null) {
                sqlUpdateStatement.setWhere(sqlBinaryOpExpr);
                sqlUpdateStatement.addWhere(where);
            }
            return SQLUtils.toSQLString(sqlUpdateStatement);
        } else {
            // 拼接条件，有点特殊处理，如果实际删除调用的话，不需要拼接删除状态
            SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr(mapperId, simpleName, true);
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
     * @param mapperId
     * @author ouyang
     * @date 2023/5/22 9:50
     */
    private String handleSelect(SQLSelectStatement sqlStatement, String mapperId) {
        SQLSelect select = sqlStatement.getSelect();
        SQLSelectQuery query = select.getQuery();
        // 处理查询
        handleQuery(query, mapperId);
        return SQLUtils.toSQLString(query);
    }

    /***
     * 处理查询
     *
     * @param query query
     * @param mapperId
     * @author ouyang
     * @date 2023/5/23 9:25
     */
    private void handleQuery(SQLSelectQuery query, String mapperId) {
        // 区分类型  SQLSelectQueryBlock 和 SQLUnionQuery
        if (query instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) query;

            // 表名
            String simpleName = null;
            SQLTableSource from = sqlSelectQueryBlock.getFrom();
            if (from instanceof SQLExprTableSource) {
                SQLExprTableSource tableSource = (SQLExprTableSource) from;
                simpleName = tableSource.getName().getSimpleName();
            }

            // 获取where条件
            SQLExpr where = sqlSelectQueryBlock.getWhere();
            // 拼接条件
            SQLBinaryOpExpr sqlBinaryOpExpr = prepareExpr(mapperId, simpleName, false);
            if (sqlBinaryOpExpr != null) {
                sqlSelectQueryBlock.setWhere(sqlBinaryOpExpr);
                sqlSelectQueryBlock.addWhere(where);
            }
        } else if (query instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) query;
            // 递归调用
            handleQuery(sqlUnionQuery.getLeft(), mapperId);
            handleQuery(sqlUnionQuery.getRight(), mapperId);
        }
    }

    /***
     * 准备拼接条件
     *
     * @return {@link SQLBinaryOpExpr}
     * @author ouyang
     * @date 2023/5/23 14:20
     * @param mapperId mapper方法全路径
     * @param simpleName 表名
     * @param deleteFlag 是否是实际删除 true是 false不是
     */
    private SQLBinaryOpExpr prepareExpr(String mapperId, String simpleName, boolean deleteFlag) {
        // delete
        SQLBinaryOpExpr binaryOpExprDelete = null;
        if (logicDataProperties.isOpen()) {
            SQLIdentifierExpr column1ExprDelete = new SQLIdentifierExpr(logicDataProperties.getDeleteColumn());
            SQLNumericLiteralExpr value1ExprDelete = new SQLIntegerExpr(logicDataProperties.getNormalValue());
            binaryOpExprDelete = new SQLBinaryOpExpr(column1ExprDelete, SQLBinaryOperator.Equality, value1ExprDelete);
        }

        // tenant_id
        SQLBinaryOpExpr binaryOpExprTenantId = null;
        if (tenantProperties.isOpen() && !tenantIdFlag(mapperId) && !noTenantTableFlag(simpleName) && !deleteFlag) {
            SQLIdentifierExpr column1ExprTenantId = new SQLIdentifierExpr(tenantProperties.getTenantColumn());
            SQLNumericLiteralExpr value1ExprTenantId = new SQLIntegerExpr(tenantService.getTenantId());
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


    /***
     * 删除标志
     *
     * @param mapperId mapperId
     * @return {@link boolean}  true需要物理删除
     * @author ouyang
     * @date 2023/5/23 17:36
     */
    private boolean deleteFlag(String mapperId) {
        List<String> deleteIds = noProcessingRequiredProperties.getDeleteIds();
        if (deleteIds == null) {
            return false;
        }
        return deleteIds.contains(mapperId);
    }

    /***
     * 租户id标志
     *
     * @param mapperId mapperId
     * @return {@link boolean} true不需拼接租户
     * @author ouyang
     * @date 2023/5/23 17:36
     */
    private boolean tenantIdFlag(String mapperId) {
        List<String> noTenantIds = noProcessingRequiredProperties.getNoTenantIds();
        if (noTenantIds == null) {
            return false;
        }
        return noTenantIds.contains(mapperId);
    }

    /***
     * 没有租户字段的表
     *
     * @param tableName 表名
     * @return {@link boolean} true不需拼接租户
     * @author ouyang
     * @date 2023/5/23 17:36
     */
    private boolean noTenantTableFlag(String tableName) {
        if (tableName == null) {
            return false;
        }
        List<String> noTenantTables = noProcessingRequiredProperties.getNoTenantTables();
        if (noTenantTables == null) {
            return false;
        }
        return noTenantTables.contains(tableName);
    }


}
