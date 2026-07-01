package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfluxDBConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxDBConfig.class);


    public String getName() { return "InfluxDBConfig"; }
    public String getCategory() { return "database"; }
    public String getDescription() { return "Extract InfluxDB time-series database connection and credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // InfluxDBImpl
            Object clazz1 = heapHolder.findClass("org.influxdb.impl.InfluxDBImpl");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[InfluxDB] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // InfluxDBProperties (Spring Boot)
            Object clazz2 = heapHolder.findClass("org.springframework.boot.autoconfigure.influx.InfluxDbProperties");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("url", "url");
                    put("username", "username");
                    put("password", "password");
                    put("database", "database");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[InfluxDBProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
