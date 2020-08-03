package com.xxl.job.executor.core.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = -100)
@Slf4j
@Aspect
public class DataSourceSwitchAspect {

    @Pointcut("execution(* com.xxl.job.executor.dao.db70..*.*(..))")
    private void db70Aspect() {
    }

    @Pointcut("execution(* com.xxl.job.executor.dao.db74..*.*(..))")
    private void db74Aspect() {
    }

    @Before("db70Aspect()")
    public void db70() {
        log.info("切换到db70 数据源...");
        DbContextHolder.setDbType(DBTypeEnum.db70);
    }

    @Before("db74Aspect()")
    public void db74() {
        log.info("切换到db74 数据源...");
        DbContextHolder.setDbType(DBTypeEnum.db74);
    }

}