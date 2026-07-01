package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeignClientConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignClientConfig.class);


    public String getName() { return "FeignClientConfig"; }
    public String getCategory() { return "rpc"; }
    public String getDescription() { return "Extract OpenFeign client URLs and configurations"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // Feign client properties
            Object clazz1 = heapHolder.findClass("org.springframework.cloud.openfeign.FeignClientProperties");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[FeignClientProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
            // Ribbon server lists (often contain internal service URLs)
            Object clazz2 = heapHolder.findClass("com.netflix.loadbalancer.DynamicServerListLoadBalancer");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    Object name = heapHolder.getFieldStringValue(instance, "name");
                    result.append("[RibbonLoadBalancer] name=").append(name).append("\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
