package com.xxl.job.executor.core.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import groovy.util.logging.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 分页插件
 * @author: Think
 * @date: 2020-01-05 12:09
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    @Bean(name = "db70")
    @ConfigurationProperties(prefix = "spring.datasource.druid.db70")
    public DataSource db70() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean(name = "db74")
    @ConfigurationProperties(prefix = "spring.datasource.druid.db74")
    public DataSource db74() {
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * 动态数据源配置
     *
     * @return
     */
    @Bean
    @Primary
    public DataSource multipleDataSource(@Qualifier("db70") DataSource db70,
                                         @Qualifier("db74") DataSource db74) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DBTypeEnum.db70.getValue(), db70);
        targetDataSources.put(DBTypeEnum.db74.getValue(), db74);
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(db70); // 设置默认数据源
        return dynamicDataSource;
    }

    @Bean("sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        MybatisSqlSessionFactoryBean sqlSessionFactory = new MybatisSqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(multipleDataSource(db70(), db74()));

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(true);
        //懒加载
        configuration.setAggressiveLazyLoading(true);
        // 打印sql
         configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);

        CountSqlInterceptor countSqlInterceptor = new CountSqlInterceptor() ;
        MyPaginationInterceptor paginationInterceptor = new MyPaginationInterceptor();
        paginationInterceptor.setCountSqlParser(countSqlInterceptor);
        // 放置自定义分页
        configuration.addInterceptor(paginationInterceptor);


        sqlSessionFactory.setConfiguration(configuration);
//        sqlSessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
//                .getResources("classpath:/mapper/**/**.xml"));
        //PerformanceInterceptor() 性能分析,OptimisticLockerInterceptor() 乐观锁
        //添加分页功能
//        sqlSessionFactory.setPlugins(new Interceptor[]{
//                paginationInterceptor().setCountSqlParser(new JsqlParserCountOptimize(true))
//        });

        return sqlSessionFactory.getObject();
    }
}
