package com.xxl.job.executor.service.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.executor.dao.PrimarykeyMapper;
import com.xxl.job.executor.vo.checkVo;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.xxl.job.core.biz.model.ReturnT.*;

/**
 * @description: 数据校验JOB
 * @author: liyue
 * @date: 2020/7/30 10:39
 */
@Component
public class checkIdXxljob {
    private static Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);
    public static final String CHECK_ID_XML_PATH = "D:\\Users\\mywork\\hxxgithub\\test01\\test002\\xxl-job\\xxl-job-executor-samples\\xxl-job-executor-sample-springboot\\src\\main\\java\\com\\xxl\\job\\executor\\service\\jobhandler\\checkXml\\checkId.xml";
    public static final String CHECK_PAT_ASSOCIATED_XML_PATH = "D:\\Users\\mywork\\hxxgithub\\test01\\test002\\xxl-job\\xxl-job-executor-samples\\xxl-job-executor-sample-springboot\\src\\main\\java\\com\\xxl\\job\\executor\\service\\jobhandler\\checkXml\\patAssociated.xml";


    @Autowired
    PrimarykeyMapper primarykeyMapper;
    ReturnT returnT = new ReturnT();

    // 校验数据ID一致性
    @XxlJob("checkIdHandler")
    public ReturnT<String> demoJobHandler(String param) throws Exception {
        int errCount = 0;
        String errTable ="";
        // 获取需要校验的SQL集合
        List<String> sqlList = getSqlList(CHECK_ID_XML_PATH);
        // 执行SQL 判断结果是否符合预期
        for (String sql : sqlList) {
            List<checkVo> voList = primarykeyMapper.checkId(sql);
            for (checkVo vo :voList) {
                if (vo.getCountNumber() > 1) {
                    errCount ++;
                    String[] arr = sql.split("from");
                    String[] arr1 = arr[1].split("group");
                    errTable += arr1[0] +" /tr";
                    break;
                }
            }
        }

        if(errCount>0){
            return new ReturnT(FAIL_CODE, errTable, CHECK_ID);
        }

        return SUCCESS;
    }

    // 校验数据患者关联性
    @XxlJob("checkPatHandler")
    public ReturnT<String> checkPatHandler(String param) throws Exception {
        int errCount = 0;
        String errTable ="";
        // 获取需要校验的SQL集合
        List<String> sqlList = getSqlList(CHECK_PAT_ASSOCIATED_XML_PATH);
        // 执行SQL 判断结果是否符合预期
        for (String sql : sqlList) {
            List<checkVo> voList = primarykeyMapper.checkId(sql);
            for (checkVo vo :voList) {
                if (vo.getCountNumber() > 1) {
                    errCount ++;
                    String[] arr = sql.split("from");
                    String[] arr1 = arr[1].split("group");
                    errTable += arr1[0] +" /tr";
                    break;
                }
            }
        }

        if(errCount>0){
            return new ReturnT(FAIL_CODE, errTable, CHECK_ID);
        }

        return SUCCESS;
    }
    // 解析 xml 获取sql队列
    public List<String> getSqlList(String xmlPath) {
        List<String> sqlList = new ArrayList<>();
        // 1.创建SAXReader的对象reader
        SAXReader reader = new SAXReader();
        try {
            // 2.通过reader对象的read（）方法加载books.xml文件，获取document对象
            Document document = reader.read(new File(xmlPath));

            // 3.通过document对象获取根节点bookstore
            Element root = document.getRootElement();

            List<Element> list = root.elements("sql");

            for (Element attr : list) {
                sqlList.add(attr.getStringValue());
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return sqlList;
    }
}