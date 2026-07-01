package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jConfig.class);


    public String getName() { return "Neo4jConfig"; }
    public String getCategory() { return "database"; }
    public String getDescription() { return "Extract Neo4j graph database connection and authentication configuration"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // InternalDriver
            Object clazz1 = heapHolder.findClass("org.neo4j.driver.internal.InternalDriver");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[Neo4jDriver] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // DirectDriver / RoutingDriver
            Object clazz2 = heapHolder.findClass("org.neo4j.driver.internal.DirectDriver");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("org.neo4j.driver.internal.RoutingDriver");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[Neo4jDriver: ").append(heapHolder.getClassName(clazz2)).append("] ")
                            .append(HashMapUtils.dumpString(vals, false));
                }
            }
            // Neo4jProperties (Spring Boot)
            Object clazz3 = heapHolder.findClass("org.springframework.boot.autoconfigure.neo4j.Neo4jProperties");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[Neo4jProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
