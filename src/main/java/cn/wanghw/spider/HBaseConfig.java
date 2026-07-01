package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HBaseConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseConfig.class);


    public String getName() { return "HBaseConfig"; }
    public String getCategory() { return "database"; }
    public String getDescription() { return "Extract Apache HBase connection configuration and credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ConnectionImpl
            Object clazz1 = heapHolder.findClass("org.apache.hadoop.hbase.client.ConnectionImpl");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HBaseConnection] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // HBaseConfiguration
            Object clazz2 = heapHolder.findClass("org.apache.hadoop.hbase.HBaseConfiguration");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HBaseConfiguration] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // ConnectionFactory / HBaseSettings
            Object clazz3 = heapHolder.findClass("org.apache.hadoop.hbase.mapreduce.TableInputFormat");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HBaseTableInputFormat] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
