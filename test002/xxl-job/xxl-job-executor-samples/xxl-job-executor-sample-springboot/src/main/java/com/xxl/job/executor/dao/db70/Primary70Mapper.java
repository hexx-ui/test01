package com.xxl.job.executor.dao.db70;

import com.xxl.job.executor.vo.checkVo;
import org.apache.ibatis.annotations.*;

import java.util.HashMap;
import java.util.List;

/**
 * @description: 主键校验
 * @author: liyue
 * @date: 2020/7/29 17:24
 */
@Mapper
public interface Primary70Mapper {

    /**
     * 70查询SQL
     * @param sql
     * @return
     */
    @Select("<script>${sql}</script>")
    @Options(flushCache = Options.FlushCachePolicy.FALSE,useCache = false,timeout = 10000)
    List<checkVo> checkdb70(@Param("sql") String sql);

    /**
     * 74数据查询
     * @param sql
     * @return
     */
    @Select("<script>${sql}</script>")
    @Options(flushCache = Options.FlushCachePolicy.FALSE,useCache = false,timeout = 10000)
    List<HashMap<String,String>> SelectHashMapList(@Param("sql") String sql);

    /**
     * 70库插入SQL
     * @param sql
     * @return
     */
    @Insert("<script>${sql}</script>")
    @Options(flushCache = Options.FlushCachePolicy.FALSE,useCache = false,timeout = 10000)
    Integer inserTDB70(@Param("sql") String sql);
}
