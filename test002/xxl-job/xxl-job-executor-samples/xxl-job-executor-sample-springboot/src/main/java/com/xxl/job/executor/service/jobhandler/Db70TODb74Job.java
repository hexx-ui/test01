package com.xxl.job.executor.service.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.dao.db70.Primary70Mapper;
import com.xxl.job.executor.dao.db74.Primary74Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

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

                List<HashMap<String, Object>> rs = primary70Mapper.Select70HashMapList(selectSql);

                if(rs == null || rs.size() <= 0){
                    returnT.setCode(FAIL_CODE);
                    returnT.setMsg(CHECK_ID +"无数据");
                    return returnT;
                }

                for (HashMap<String, Object> hashMap : rs){

                    // 组装成插入74的数据
                    StringBuffer insertSql = new StringBuffer();
                    StringBuffer valueSql = new StringBuffer();
                    // 组装校验数据
                    StringBuffer haveSql = new StringBuffer();

                    insertSql.append(" Insert into "+ tableName +"  (   ");

                    //  KEY 即 表字段，value 即 字段值
                    for(String key : hashMap.keySet()){
                        insertSql.append(key+",");
                        valueSql.append(getMapValue(hashMap.get(key))+",");
                        haveSql.append(key +"="+  getMapValue(hashMap.get(key))+" and ");
                    }
                    String rsSQL = insertSql.toString().substring(0, insertSql.length() - 1) + " ) values ( "
                                 + valueSql.toString().substring(0, valueSql.length() - 1) + " )";


                    // 判断是否已存在该数据
                    List<HashMap<String,Object>> number = primary74Mapper.Select74HashMapList(" SELECT * FROM " + tableName
                            + " where "+ haveSql.toString().substring(0,haveSql.length() - 4));

                    if(number != null && number.size()>0 ){
                        continue;
                    }
                    // 插入数据库
                    System.out.println(rsSQL);
                    primary74Mapper.inserTDB74(rsSQL);
                }

            }catch (Exception e){
                returnT.setCode(FAIL_CODE);
                returnT.setMsg(e.getMessage() +" 70 TO 74 数据调度 出现错误！");
                return returnT;
            }

        }
        returnT.setCode(SUCCESS_CODE);
        returnT.setMsg("数据调度成功!");
        return returnT;
    }

    private String getMapValue(Object o) {
        if (o == null) {
            return "''";
        } else if (isNumber(o.toString())) {
            return o.toString();
        } else {
            return " '"+o.toString()+"' ";
        }
    }
    /**
     * 判断一个字符串是否是数字。
     *
     * @param string
     * @return
     */
    public  boolean isNumber(String string) {
        if (string == null)
            return false;
        Pattern pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$");
        return pattern.matcher(string).matches();
    }


    private String[] getTableName() {
        return new String[]{
                "tb_area"
        };
    }

}
