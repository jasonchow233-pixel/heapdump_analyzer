package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCloudSentinelConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudSentinelConfig.class);


    public String getName() { return "SpringCloudSentinelConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract Alibaba Sentinel dashboard and flow rule configurations"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // SentinelConfig
            Object clazz1 = heapHolder.findClass("com.alibaba.csp.sentinel.config.SentinelConfig");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[SentinelConfig] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // FlowRuleManager
            Object clazz2 = heapHolder.findClass("com.alibaba.csp.sentinel.slots.block.flow.FlowRule");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("resource", "resource");
                    put("limitApp", "limitApp");
                    put("count", "count");
                    put("grade", "grade");
                    put("strategy", "strategy");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[FlowRule] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // SentinelProperties (Spring Cloud)
            Object clazz3 = heapHolder.findClass("com.alibaba.cloud.sentinel.SentinelProperties");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[SentinelProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
