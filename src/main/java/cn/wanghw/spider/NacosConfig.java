package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NacosConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfig.class);


    public String getName() {
        return "NacosConfig";
    }

    public String getCategory() {
        return "registry";
    }

    public String getDescription() {
        return "Extract Nacos configuration center and service discovery credentials";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.HIGH;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // NacosConfigProperties (already partially covered by UserPassSearcher01, but this is more thorough)
            Object clazz1 = heapHolder.findClass("com.alibaba.cloud.nacos.NacosConfigProperties");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.alibaba.nacos.client.config.NacosConfigProperties");
            if (clazz1 != null) {
                HashMap<String, String> fieldList1 = new HashMap<String, String>() {{
                    put("serverAddr", "serverAddr");
                    put("username", "username");
                    put("password", "password");
                    put("namespace", "namespace");
                    put("group", "group");
                    put("accessKey", "accessKey");
                    put("secretKey", "secretKey");
                    put("contextPath", "contextPath");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[NacosConfigProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList1), false));
                }
            }
            // NacosDiscoveryProperties
            Object clazz2 = heapHolder.findClass("com.alibaba.cloud.nacos.NacosDiscoveryProperties");
            if (clazz2 != null) {
                HashMap<String, String> fieldList2 = new HashMap<String, String>() {{
                    put("serverAddr", "serverAddr");
                    put("username", "username");
                    put("password", "password");
                    put("namespace", "namespace");
                    put("accessKey", "accessKey");
                    put("secretKey", "secretKey");
                    put("service", "service");
                    put("ip", "ip");
                    put("port", "port");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[NacosDiscoveryProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList2), false));
                }
            }
            // NacosNamingService
            Object clazz3 = heapHolder.findClass("com.alibaba.nacos.client.naming.NacosNamingService");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    Object nsProps = heapHolder.getFieldValue(instance, "namespace");
                    result.append("[NacosNamingService] namespace=").append(nsProps != null ? nsProps.toString() : "null");
                    Object serverProxy = heapHolder.getFieldValue(instance, "serverProxy");
                    if (serverProxy != null) {
                        String serverAddr = heapHolder.getFieldStringValue(serverProxy, "serverList");
                        result.append(", serverList=").append(serverAddr != null ? serverAddr : "null");
                    }
                    result.append("\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
