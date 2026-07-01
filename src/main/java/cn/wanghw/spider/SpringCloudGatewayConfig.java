package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCloudGatewayConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudGatewayConfig.class);


    public String getName() { return "SpringCloudGatewayConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract Spring Cloud Gateway route definitions and filters"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // RouteDefinition
            Object clazz = heapHolder.findClass("org.springframework.cloud.gateway.route.RouteDefinition");
            if (clazz != null) {
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[RouteDefinition] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // GatewayProperties
            Object clazz2 = heapHolder.findClass("org.springframework.cloud.gateway.config.GatewayProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[GatewayProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // GlobalFilter adapters
            Object clazz3 = heapHolder.findClass("org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    String name = heapHolder.getFieldStringValue(instance, "name");
                    if (name != null) {
                        result.append("[GatewayFilter] name=").append(name).append("\n");
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
