package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCloudConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudConfig.class);


    public String getName() {
        return "SpringCloudConfig";
    }

    public String getCategory() {
        return "config";
    }

    public String getDescription() {
        return "Extract Spring Cloud Config server/client properties";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.HIGH;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ConfigClientProperties
            Object clazz1 = heapHolder.findClass("org.springframework.cloud.config.client.ConfigClientProperties");
            if (clazz1 != null) {
                HashMap<String, String> fieldList1 = new HashMap<String, String>() {{
                    put("uri", "uri");
                    put("username", "username");
                    put("password", "password");
                    put("name", "name");
                    put("profile", "profile");
                    put("label", "label");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[ConfigClientProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList1), false));
                }
            }
            // ConfigServerProperties
            Object clazz2 = heapHolder.findClass("org.springframework.cloud.config.server.config.ConfigServerProperties");
            if (clazz2 != null) {
                HashMap<String, String> fieldList2 = new HashMap<String, String>() {{
                    put("defaultLabel", "defaultLabel");
                    put("defaultProfile", "defaultProfile");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[ConfigServerProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList2), false));
                }
            }
            // Git properties for config server
            Object clazz3 = heapHolder.findClass("org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties");
            if (clazz3 != null) {
                HashMap<String, String> fieldList3 = new HashMap<String, String>() {{
                    put("uri", "uri");
                    put("username", "username");
                    put("password", "password");
                    put("searchPaths", "searchPaths");
                }};
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    result.append("[GitConfigSource] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList3), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
