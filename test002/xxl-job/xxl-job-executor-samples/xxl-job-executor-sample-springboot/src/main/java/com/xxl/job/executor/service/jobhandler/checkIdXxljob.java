package com.xxl.job.executor.service.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.dao.db70.Primary70Mapper;
import com.xxl.job.executor.dao.db74.Primary74Mapper;
import com.xxl.job.executor.vo.checkVo;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.xxl.job.core.biz.model.ReturnT.FAIL_CODE;
import static com.xxl.job.core.biz.model.ReturnT.SUCCESS_CODE;

/**
 * @description: 数据校验JOB
 * @author: liyue
 * @date: 2020/7/30 10:39
 */
@Component
public class checkIdXxljob {
    private static Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);
    // 服务器
       public static final String CHECK_ID_XML_PATH = "/var/local/job/checkXml/checkId.xml";
       public static final String CHECK_PAT_ASSOCIATED_XML_PATH = "/var/local/job/checkXml/patAssociated.xml";
       public static final String TABLE_COUNT_CHECK_XML_PATH = "/var/local/job/checkXml/tableCountcheck.xml";

    // wen10
    // public static final String CHECK_ID_XML_PATH = "xxl-job-executor-samples\\xxl-job-executor-sample-springboot\\checkXml\\checkId.xml";
    // public static final String CHECK_PAT_ASSOCIATED_XML_PATH = "xxl-job-executor-samples\\xxl-job-executor-sample-springboot\\checkXml\\patAssociated.xml";
    // public static final String TABLE_COUNT_CHECK_XML_PATH = "xxl-job-executor-samples\\xxl-job-executor-sample-springboot\\checkXml\\tableCountcheck.xml";

    public static final String CHECK_ID = " 数据ID一致性错误的数据表： ";
    public static final String CHECK_Pat = " 患者关联性错误的数据表： ";
    public static final String CHECK_7074Count = " 7074表数据总量不一致的数据表： ";



    @Autowired
    private Primary70Mapper primary70Mapper;
    @Autowired
    private Primary74Mapper primary74Mapper;

    ReturnT returnT = new ReturnT();

    // 校验数据ID一致性
    @XxlJob("checkIdHandler")
    public ReturnT<String> demoJobHandler(String param) throws Exception {
        int errCount = 0;
        String errTable ="";

        // 获取需要校验的SQL集合
        List<String> sqlList = getSqlList(CHECK_ID_XML_PATH);
        System.out.println("______________________ID一致性执行SQL数"+ sqlList.size() );
        // 执行SQL 判断结果是否符合预期
        for (String sql : sqlList) {
            List<checkVo> voList = primary70Mapper.checkdb70(sql);
            for (checkVo vo : voList) {
                if (vo.getCountNumber() > 1) {
                    errCount++;
                    String[] arr = sql.split("from");
                    String[] arr1 ;
                    if (arr[1].contains("where")) {
                        arr1 = arr[1].split("where");
                    } else {
                        arr1 = arr[1].split("group");
                    }
                    errTable += arr1[0] + ",     ";
                    break;
                }
            }
        }

        if(errCount>0){
            returnT.setCode(FAIL_CODE);
            returnT.setMsg(CHECK_ID +errTable);
            return returnT;
        }else {
            returnT.setCode(SUCCESS_CODE);
            returnT.setMsg("数据ID一致性校验通过!");
            return returnT;
        }
    }

    // 校验数据患者关联性
    @XxlJob("checkPatHandler")
    public ReturnT<String> checkPatHandler(String param) throws Exception {
        int errCount = 0;
        String errTable ="";
        System.out.println("_________________________"+ System.getProperty("user.dir"));
        // 获取需要校验的SQL集合
        List<String> sqlList = getSqlList(CHECK_PAT_ASSOCIATED_XML_PATH);
        System.out.println("______________________患者关联性执行SQL数"+ sqlList.size() );
        // 执行SQL 判断结果是否符合预期
        for (String sql : sqlList) {
            List<checkVo> voList = primary70Mapper.checkdb70(sql);
            for (checkVo vo :voList) {
                if (vo == null || StringUtils.isEmpty(vo.getOrgPatientSn()) || StringUtils.isEmpty(vo.getOrgPatientSnB()) || !vo.getOrgPatientSn().equals(vo.getOrgPatientSnB())) {
                    errCount ++;
                    String[] arr = sql.split("from");
                    String[] arr1 = arr[1].split("a  left join");
                    errTable += arr1[0] +",     ";
                    break;
                }
            }
        }

        if(errCount>0){
            returnT.setCode(FAIL_CODE);
            returnT.setMsg(CHECK_Pat +errTable);
            return returnT;
        }else {
            returnT.setCode(SUCCESS_CODE);
            returnT.setMsg("数据患者关联性通过!");
            return returnT;
        }
    }

    // 校验表数据总量一致性
    @XxlJob("tableCountcheck")
    public ReturnT<String> tableCountcheck(String param) throws Exception {
        int errCount = 0;
        String errTable ="";

        // 获取需要校验的SQL集合
        List<String> sqlList = getSqlList(TABLE_COUNT_CHECK_XML_PATH);
        System.out.println("______________________表数据总量一致性执行SQL数"+ sqlList.size() );
        // 执行SQL 判断结果是否符合预期
        for (String sql : sqlList) {
            List<checkVo> voList70 = primary70Mapper.checkdb70(sql);
            List<checkVo> voList74 = primary74Mapper.checkdb74(sql);
            for (int i = 0; i < voList70.size(); i++) {
                if (voList70.get(i) != null && voList74.get(i) != null && voList70.get(i).getCountNumber() != null && voList74.get(i).getCountNumber() != null
                        && !voList70.get(i).getCountNumber().equals(voList74.get(i).getCountNumber())) {
                    errCount++;
                    String[] arr = sql.split("from");
                    String[] arr1 ;
                    if (arr[1].contains("where")) {
                        arr1 = arr[1].split("where");
                        errTable += arr1[0] + ",     ";
                    } else {
                        errTable += arr[1] + ",     ";
                    }
                    break;
                }
            }
        }

        if(errCount>0){
            returnT.setCode(FAIL_CODE);
            returnT.setMsg(CHECK_7074Count +errTable);
            return returnT;
        }else {
            returnT.setCode(SUCCESS_CODE);
            returnT.setMsg("表数据总量一致性校验通过!");
            return returnT;
        }
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