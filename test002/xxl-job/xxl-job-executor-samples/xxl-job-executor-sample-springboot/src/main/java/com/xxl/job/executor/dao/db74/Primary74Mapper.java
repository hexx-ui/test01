package com.xxl.job.executor.dao.db74;

import com.xxl.job.executor.vo.checkVo;
import org.apache.ibatis.annotations.*;

import java.util.HashMap;
import java.util.List;

/**
 * @description: 主键校验
 * @author: liyue
 * @date: 2020/12/12 17:24
 */
@Mapper
public interface Primary74Mapper {

    /**
     * 校验主键一致性
     * @param sql
     * @return
     */
    @Select("<script>${sql}</script>")
    @Options(flushCache = Options.FlushCachePolicy.FALSE,useCache = false,timeout = 10000)
    List<checkVo> checkdb74(@Param("sql") String sql);

    /**
     * 74数据查询
     * @param sql
     * @return
     */
    @Select("<script>${sql}</script>")
    @Options(flushCache = Options.FlushCachePolicy.FALSE,useCache = false,timeout = 10000)
    List<HashMap<String,Object>> Select74HashMapList(@Param("sql") String sql);

    /**
     * 74库插入SQL
     * @param sql
     * @return
     */
    @Insert("<script>${sql}</script>")
    @Options(flushCache = Options.FlushCachePolicy.FALSE,useCache = false,timeout = 10000)
    Integer inserTDB74(@Param("sql") String sql);
}
