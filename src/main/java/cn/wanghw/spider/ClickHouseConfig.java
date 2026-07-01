package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickHouseConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseConfig.class);


    public String getName() { return "ClickHouseConfig"; }
    public String getCategory() { return "database"; }
    public String getDescription() { return "Extract ClickHouse JDBC/data source configuration and credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ClickHouseDataSource
            Object clazz1 = heapHolder.findClass("com.clickhouse.jdbc.ClickHouseDataSource");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.github.housepower.jdbc.ClickHouseDataSource");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[ClickHouseDataSource] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // ClickHouseProperties
            Object clazz2 = heapHolder.findClass("com.clickhouse.jdbc.ClickHouseProperties");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("com.github.housepower.jdbc.settings.ClickHouseProperties");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("url", "url");
                    put("user", "user");
                    put("password", "password");
                    put("database", "database");
                    put("host", "host");
                    put("port", "port");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[ClickHouseProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
