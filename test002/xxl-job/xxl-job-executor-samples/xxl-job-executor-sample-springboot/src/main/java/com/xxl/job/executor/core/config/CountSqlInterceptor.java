package com.xxl.job.executor.core.config;

import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.SqlInfo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.reflection.MetaObject;

import java.util.ArrayList;
import java.util.List;

public class CountSqlInterceptor implements ISqlParser {




    private static final List<SelectItem> COUNT_SELECT_ITEM = countSelectItem();
    /**
     * 日志
     */
    private final Log logger = LogFactory.getLog(CountSqlInterceptor.class);

    /**
     * 获取jsqlparser中count的SelectItem
     */
    private static List<SelectItem> countSelectItem() {
        Function function = new Function();
        function.setName("COUNT");
        List<Expression> expressions = new ArrayList<>();
        LongValue longValue = new LongValue(1);
        ExpressionList expressionList = new ExpressionList();
        expressions.add(longValue);
        expressionList.setExpressions(expressions);
        function.setParameters(expressionList);
        List<SelectItem> selectItems = new ArrayList<>();
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem(function);
        selectItems.add(selectExpressionItem);
        return selectItems;
    }

    @Override
    public SqlInfo parser(MetaObject metaObject, String sql) {
        if (logger.isDebugEnabled()) {
            logger.debug("JsqlParserCountOptimize sql=" + sql);
        }
        SqlInfo sqlInfo = SqlInfo.newInstance();
        try {
            Select selectStatement = (Select) CCJSqlParserUtil.parse(sql);
            PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
            Distinct distinct = plainSelect.getDistinct();
            GroupByElement groupBy = plainSelect.getGroupBy();
            List<OrderByElement> orderBy = plainSelect.getOrderByElements();
            List<Join> joins = plainSelect.getJoins();
            // 添加包含groupBy 不去除orderBy
            if (groupBy != null && CollectionUtils.isNotEmpty(orderBy)) {
                plainSelect.setOrderByElements(null);
                sqlInfo.setOrderBy(false);
            }
            //#95 Github, selectItems contains #{} ${}, which will be translated to ?, and it may be in a function: power(#{myInt},2)
            for (SelectItem item : plainSelect.getSelectItems()) {
                if (item.toString().contains(StringPool.QUESTION_MARK)) {
                    return sqlInfo.setSql(SqlParserUtils.getOriginalCountSql(selectStatement.toString()));
                }
            }
            // 包含 distinct、groupBy不优化
            if (distinct != null || groupBy != null) {
                return sqlInfo.setSql(SqlParserUtils.getOriginalCountSql(selectStatement.toString()));
            }

            /**
             *  此处很关键，判断如果有作连接，则去掉作连接内容，然后根据主表进行分页总数的计算
             * 如果有join并且是左连接，则说明此为主表
             */
            if(joins !=null && joins.get(0).isLeft()){
                plainSelect.setJoins(null);
            }

            // 优化 SQL
            plainSelect.setSelectItems(COUNT_SELECT_ITEM);
            return sqlInfo.setSql(selectStatement.toString());
        } catch (Throwable e) {
            // 无法优化使用原 SQL
            return sqlInfo.setSql(SqlParserUtils.getOriginalCountSql(sql));
        }

    }
}