package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HikariCPDetail implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariCPDetail.class);


    public String getName() { return "HikariCPDetail"; }
    public String getCategory() { return "database"; }
    public String getDescription() { return "Extract HikariCP connection pool detailed configuration including credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // HikariConfig
            Object clazz1 = heapHolder.findClass("com.zaxxer.hikari.HikariConfig");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("jdbcUrl", "jdbcUrl");
                    put("username", "username");
                    put("password", "password");
                    put("driverClassName", "driverClassName");
                    put("poolName", "poolName");
                    put("maximumPoolSize", "maximumPoolSize");
                    put("minimumIdle", "minimumIdle");
                    put("connectionTimeout", "connectionTimeout");
                    put("dataSourceClassName", "dataSourceClassName");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[HikariConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // HikariDataSource (extends HikariConfig)
            Object clazz2 = heapHolder.findClass("com.zaxxer.hikari.HikariDataSource");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HikariDataSource] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
