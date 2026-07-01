package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DubboConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DubboConfig.class);


    public String getName() {
        return "DubboConfig";
    }

    public String getCategory() {
        return "rpc";
    }

    public String getDescription() {
        return "Extract Apache Dubbo registry and protocol configuration";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.HIGH;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // Dubbo RegistryConfig
            Object clazz1 = heapHolder.findClass("org.apache.dubbo.config.RegistryConfig");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.alibaba.dubbo.config.RegistryConfig");
            if (clazz1 != null) {
                HashMap<String, String> fieldList1 = new HashMap<String, String>() {{
                    put("address", "address");
                    put("username", "username");
                    put("password", "password");
                    put("protocol", "protocol");
                    put("group", "group");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[RegistryConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList1), false));
                }
            }
            // Dubbo ProtocolConfig
            Object clazz2 = heapHolder.findClass("org.apache.dubbo.config.ProtocolConfig");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("com.alibaba.dubbo.config.ProtocolConfig");
            if (clazz2 != null) {
                HashMap<String, String> fieldList2 = new HashMap<String, String>() {{
                    put("name", "name");
                    put("host", "host");
                    put("port", "port");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[ProtocolConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList2), false));
                }
            }
            // Dubbo ApplicationConfig
            Object clazz3 = heapHolder.findClass("org.apache.dubbo.config.ApplicationConfig");
            if (clazz3 == null)
                clazz3 = heapHolder.findClass("com.alibaba.dubbo.config.ApplicationConfig");
            if (clazz3 != null) {
                HashMap<String, String> fieldList3 = new HashMap<String, String>() {{
                    put("name", "name");
                    put("owner", "owner");
                }};
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    result.append("[ApplicationConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList3), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
