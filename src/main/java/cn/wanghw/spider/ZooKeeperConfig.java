package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperConfig.class);


    public String getName() {
        return "ZooKeeperConfig";
    }

    public String getCategory() {
        return "registry";
    }

    public String getDescription() {
        return "Extract ZooKeeper connection strings and Curator configuration";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.MEDIUM;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // CuratorFramework
            Object clazz1 = heapHolder.findClass("org.apache.curator.framework.CuratorFrameworkImpl");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    Object state = heapHolder.getFieldStringValue(instance, "state");
                    Object zkClient = heapHolder.getFieldValue(instance, "zkClient");
                    if (zkClient != null) {
                        String connectionStr = heapHolder.getFieldStringValue(zkClient, "connectionString");
                        result.append("[CuratorFramework] state=").append(state)
                                .append(", connectionString=").append(connectionStr).append("\n");
                    }
                }
            }
            // ZooKeeper
            Object clazz2 = heapHolder.findClass("org.apache.zookeeper.ZooKeeper");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    String connectStr = heapHolder.getFieldStringValue(instance, "connectString");
                    String state = heapHolder.getFieldStringValue(instance, "state");
                    result.append("[ZooKeeper] connectString=").append(connectStr)
                            .append(", state=").append(state).append("\n");
                }
            }
            // Spring Cloud ZooKeeper properties
            Object clazz3 = heapHolder.findClass("org.springframework.cloud.zookeeper.ZookeeperProperties");
            if (clazz3 != null) {
                HashMap<String, String> fieldList3 = new HashMap<String, String>() {{
                    put("connectString", "connectString");
                    put("basePath", "basePath");
                }};
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    result.append("[ZookeeperProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList3), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
