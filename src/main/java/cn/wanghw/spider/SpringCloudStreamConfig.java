package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCloudStreamConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudStreamConfig.class);


    public String getName() { return "SpringCloudStreamConfig"; }
    public String getCategory() { return "mq"; }
    public String getDescription() { return "Extract Spring Cloud Stream binder and binding configurations"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // BindingServiceProperties
            Object clazz1 = heapHolder.findClass("org.springframework.cloud.stream.binding.BindingServiceProperties");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[StreamBindingProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // BinderConfiguration
            Object clazz2 = heapHolder.findClass("org.springframework.cloud.stream.binder.BinderConfiguration");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[BinderConfiguration] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
