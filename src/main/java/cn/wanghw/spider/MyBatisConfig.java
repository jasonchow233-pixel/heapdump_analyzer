package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyBatisConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyBatisConfig.class);


    public String getName() { return "MyBatisConfig"; }
    public String getCategory() { return "database"; }
    public String getDescription() { return "Extract MyBatis/MyBatis-Plus configuration and SQL mappings"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // Configuration (MyBatis)
            Object clazz1 = heapHolder.findClass("org.apache.ibatis.session.Configuration");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("databaseId", "databaseId");
                    put("defaultExecutorType", "defaultExecutorType");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[MyBatisConfiguration] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // MybatisPlusProperties
            Object clazz2 = heapHolder.findClass("com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[MyBatisPlusProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // SqlSessionFactoryBean / SqlSessionFactory
            Object clazz3 = heapHolder.findClass("org.mybatis.spring.SqlSessionFactoryBean");
            if (clazz3 == null)
                clazz3 = heapHolder.findClass("org.apache.ibatis.session.defaults.DefaultSqlSessionFactory");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[SqlSessionFactory] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
