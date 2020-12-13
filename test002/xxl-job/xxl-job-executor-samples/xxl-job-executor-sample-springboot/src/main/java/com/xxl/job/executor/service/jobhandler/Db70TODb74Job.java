package com.xxl.job.executor.service.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.dao.db70.Primary70Mapper;
import com.xxl.job.executor.dao.db74.Primary74Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static com.xxl.job.core.biz.model.ReturnT.FAIL_CODE;
import static com.xxl.job.core.biz.model.ReturnT.SUCCESS_CODE;
import static com.xxl.job.executor.service.jobhandler.checkIdXxljob.CHECK_ID;

/**
 * @description: Db70TODb74Job
 * @author: liyue
 * @date: 2020/12/12 16:56
 */
@Component
public class Db70TODb74Job {
    @Autowired
    private Primary70Mapper primary70Mapper;
    @Autowired
    private Primary74Mapper primary74Mapper;

    ReturnT returnT = new ReturnT();
    // 测试
    @XxlJob("testHandler")
    public ReturnT<String> demoJobHandler(String param) throws Exception {

        // 此方法里配置需要调度的表名，两个库都要有此表并且表结构相同
        String[] tavkeNames =  getTableName();

        for(String tableName : tavkeNames){
            try {
                // 查70需要调度的数据
                String selectSql =  " select * from "+tableName ;
                System.out.println(selectSql);

                List<HashMap<String, String>> rs = primary70Mapper.SelectHashMapList(selectSql);

                if(rs == null || rs.size() <= 0){
                    returnT.setCode(FAIL_CODE);
                    returnT.setMsg(CHECK_ID +"无数据");
                    return returnT;
                }

                // 组装成插入74的数据
                StringBuffer sql = new StringBuffer();
                StringBuffer valueSql = new StringBuffer();

                sql.append(" Insert into "+ tableName +"  (   ");

                for (HashMap<String, String> hashMap : rs){
                    //  KEY 即 表字段，value 即 字段值
                    for(String key : hashMap.keySet()){
                        sql.append(key+", ");
                        valueSql.append(" '"+hashMap.get(key)+ "',");
                    }
                    String rsSQL = sql.toString().substring(0, sql.length() - 1) + " ) values ( "
                                 + valueSql.toString().substring(0, valueSql.length() - 1) + " )";

                    System.out.println(rsSQL);
                    // 插入数据库
                    primary74Mapper.inserTDB74(rsSQL);
                }

            }catch (Exception e){
                returnT.setCode(FAIL_CODE);
                returnT.setMsg(e.getMessage() +" 70 TO 74 数据调度 出现错误！");
                return returnT;
            }

        }
        returnT.setCode(SUCCESS_CODE);
        returnT.setMsg("数据ID一致性校验通过!");
        return returnT;
    }

    private String[] getTableName() {
        return new String[]{
                "tb_area"
        };
    }
}
