package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApolloConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloConfig.class);


    public String getName() { return "ApolloConfig"; }
    public String getCategory() { return "registry"; }
    public String getDescription() { return "Extract Apollo configuration center credentials and namespaces"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ApolloConfigProperties
            Object clazz1 = heapHolder.findClass("com.ctrip.framework.apollo.spring.config.ApolloConfigProperties");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.ctrip.framework.apollo.spring.boot.ApolloConfigProperties");
            if (clazz1 != null) {
                HashMap<String, String> fieldList1 = new HashMap<String, String>() {{
                    put("metaServer", "metaServer");
                    put("accessKey", "accessKey");
                    put("secretKey", "secretKey");
                    put("namespace", "namespace");
                    put("cluster", "cluster");
                    put("bootstrapEnabled", "bootstrapEnabled");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[ApolloConfigProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList1), false));
                }
            }
            // ApolloClientConfig
            Object clazz2 = heapHolder.findClass("com.ctrip.framework.apollo.internals.RemoteConfigRepository");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[ApolloRemoteConfig] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // ServiceDTO (Apollo meta server addresses)
            Object clazz3 = heapHolder.findClass("com.ctrip.framework.apollo.core.dto.ServiceDTO");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    String homepageUrl = heapHolder.getFieldStringValue(instance, "homepageUrl");
                    String appName = heapHolder.getFieldStringValue(instance, "appName");
                    result.append("[ApolloService] appName=").append(appName)
                            .append(", homepageUrl=").append(homepageUrl).append("\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
