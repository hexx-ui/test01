package com.xxl.job.executor.core.config;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisDefaultParameterHandler;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.SqlInfo;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.DialectFactory;
import com.baomidou.mybatisplus.extension.plugins.pagination.DialectModel;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Setter
@Accessors(chain = true)
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MyPaginationInterceptor extends PaginationInterceptor {


    /**
     * COUNT SQL 解析
     */
    private ISqlParser countSqlParser;
    /**
     * 溢出总页数，设置第一页
     */
    private boolean overflow = false;
    /**
     * 单页限制 500 条，小于 0 如 -1 不受限制
     */
    private long limit = 500L;
    /**
     * 方言类型
     */
    private String dialectType;
    /**
     * 方言实现类
     */
    private String dialectClazz;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // SQL 解析
        this.sqlParser(metaObject);

        // 先判断是不是SELECT操作  (2019-04-10 00:37:31 跳过存储过程)
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (SqlCommandType.SELECT != mappedStatement.getSqlCommandType()
                || StatementType.CALLABLE == mappedStatement.getStatementType()) {
            return invocation.proceed();
        }

        // 针对定义了rowBounds，做为mapper接口方法的参数
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        Object paramObj = boundSql.getParameterObject();

        // 判断参数里是否有page对象
        IPage<?> page = null;
        if (paramObj instanceof IPage) {
            page = (IPage<?>) paramObj;
        } else if (paramObj instanceof Map) {
            for (Object arg : ((Map<?, ?>) paramObj).values()) {
                if (arg instanceof IPage) {
                    page = (IPage<?>) arg;
                    break;
                }
            }
        }

        /*
         * 不需要分页的场合，如果 size 小于 0 返回结果集
         */
        if (null == page || page.getSize() < 0) {
            return invocation.proceed();
        }

        /*
         * 处理单页条数限制
         */

        if (limit > 0 && limit <= page.getSize()) {
            page.setSize(limit);
        }

        String originalSql = boundSql.getSql();
        Connection connection = (Connection) invocation.getArgs()[0];
        DbType dbType = StringUtils.isNotEmpty(dialectType) ? DbType.getDbType(dialectType)
                : JdbcUtils.getDbType(connection.getMetaData().getURL());

        boolean orderBy = true;
        if (page.isSearchCount()) {
            SqlInfo sqlInfo = SqlParserUtils.getOptimizeCountSql(page.optimizeCountSql(), countSqlParser, originalSql);
            orderBy = sqlInfo.isOrderBy();
            this.queryTotal(overflow, sqlInfo.getSql(), mappedStatement, boundSql, page, connection);
            if (page.getTotal() <= 0) {
                return null;
            }
        }

        String buildSql = concatOrderBy(originalSql, page);

        Configuration configuration = mappedStatement.getConfiguration();
        List<ParameterMapping> mappings = new ArrayList<>(boundSql.getParameterMappings());
        Map<String, Object> additionalParameters = (Map<String, Object>) metaObject.getValue("delegate.boundSql.additionalParameters");


        Select selectStatement = (Select) CCJSqlParserUtil.parse(originalSql);
        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
        List<Join> joins = plainSelect.getJoins();
        //此处判断是否为多对多或者一对多的查询
        if(joins!=null && joins.size()>0&&joins.get(0).isLeft()){
            String exceptPagingWhereSql = this.getCountIdSql(plainSelect);
            DialectModel countModel = DialectFactory.buildPaginationSql(page, exceptPagingWhereSql, dbType, dialectClazz);

            countModel.consumers(mappings, configuration, additionalParameters);
            exceptPagingWhereSql= countModel.getDialectSql() ;

            BoundSql bsql = new BoundSql(configuration,exceptPagingWhereSql,mappings,boundSql.getParameterObject());
            additionalParameters.forEach((key,value)->{
                bsql.setAdditionalParameter(key,value);
            });
            List<String> ids = this.queryIds(exceptPagingWhereSql, mappedStatement, bsql, page, connection,mappings);

            return specialPaging(metaObject,originalSql,ids,invocation,boundSql);

        }



        DialectModel model = DialectFactory.buildPaginationSql(page, buildSql, dbType, dialectClazz);

        model.consumers(mappings, configuration, additionalParameters);
        metaObject.setValue("delegate.boundSql.sql", model.getDialectSql());
        metaObject.setValue("delegate.boundSql.parameterMappings", mappings);
        return invocation.proceed();
    }

    /**
     * 联合查询特殊分页处理方法
     * @param metaObject
     * @param originalSql
     * @param ids
     * @param invocation
     * @param boundSql
     * @return
     * @throws JSQLParserException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object specialPaging(MetaObject metaObject, String originalSql, List<String> ids, Invocation invocation, BoundSql boundSql) throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        Select selectStatement = (Select) CCJSqlParserUtil.parse(originalSql);
        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
        FromItem fromItem = plainSelect.getFromItem();
        String aname = fromItem.getAlias()==null?null:fromItem.getAlias().getName() ;
        Expression where = plainSelect.getWhere();
        ExpressionList list= new ExpressionList();
        List<Expression> expressions = new ArrayList<>();
        ids.forEach(item->{
            StringValue stringValue= new StringValue(item);
            expressions.add(stringValue);
        });
        list.setExpressions(expressions);

        Column id = new Column(aname==null?fromItem.toString()+".id":aname+".id");
        InExpression inExpression = new InExpression(id,list);
        AndExpression andExpression = new AndExpression(where,inExpression);
        plainSelect.setWhere(andExpression);
        metaObject.setValue("delegate.boundSql.sql", plainSelect.toString());
        List<ParameterMapping> mappings = new ArrayList<>(boundSql.getParameterMappings());
        metaObject.setValue("delegate.boundSql.parameterMappings", mappings);
        return  invocation.proceed();
    }


    public String getCountIdSql(PlainSelect plainSelect){
        String exceptPagingWhereSql=null ;
        plainSelect.setJoins(null);
        FromItem fromItem = plainSelect.getFromItem();
        Alias alias = fromItem.getAlias();
        List<SelectItem> items = new ArrayList<>();
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column("id"));
        items.add(selectExpressionItem);
        plainSelect.setSelectItems(items);
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();

        if(orderByElements==null||orderByElements.size()<=0){
            orderByElements = new ArrayList<>();
            OrderByElement orderByElement = new OrderByElement() ;
            orderByElement.setAsc(true);
            orderByElement.setExpression(new Column("id"));
            orderByElements.add(orderByElement);
            plainSelect.setOrderByElements(orderByElements);
        }
        exceptPagingWhereSql = plainSelect.toString();
        return exceptPagingWhereSql ;
    }

    /**
     * 查询总记录条数
     * @param sql             count sql
     * @param mappedStatement MappedStatement
     * @param boundSql        BoundSql
     * @param page            IPage
     * @param connection      Connection
     * @param mappings
     */
    protected List<String> queryIds(String sql, MappedStatement mappedStatement, BoundSql boundSql, IPage<?> page, Connection connection, List<ParameterMapping> mappings) {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
//            boundSql.setAdditionalParameter(additionalParameters);
            Object parameterObject = boundSql.getParameterObject();
            DefaultParameterHandler parameterHandler = new MybatisDefaultParameterHandler(mappedStatement, parameterObject, boundSql);
            parameterHandler.setParameters(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while(resultSet.next()){
                    ids.add(resultSet.getString(1));
                }
            }
            /*
             * 溢出总页数，设置第一页
             */
            return ids;

        } catch (Exception e) {
            throw ExceptionUtils.mpe("Error: Method queryTotal execution error of sql : \n %s \n", e, sql);
        }
    }


}