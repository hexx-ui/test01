package com.xxl.job.executor.dao;

import com.xxl.job.executor.vo.checkVo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @description: 主键校验
 * @author: liyue
 * @date: 2020/7/29 17:24
 */
@Mapper
public interface PrimarykeyMapper {

    /**
     * 校验主键一致性
     * @param sql
     * @return
     */
    @Select("<script>${sql}</script>")
    @Options(flushCache = Options.FlushCachePolicy.FALSE,useCache = false,timeout = 10000)
    List<checkVo> checkId (@Param("sql") String sql);

}
