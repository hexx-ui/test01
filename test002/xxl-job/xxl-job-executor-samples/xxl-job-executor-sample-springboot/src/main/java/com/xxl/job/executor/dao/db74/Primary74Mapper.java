package com.xxl.job.executor.dao.db74;

import com.xxl.job.executor.vo.checkVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @description: 主键校验
 * @author: liyue
 * @date: 2020/7/29 17:24
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

}
