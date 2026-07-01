package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JeecgBootConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(JeecgBootConfig.class);


    public String getName() { return "JeecgBootConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract JeecgBoot low-code platform configuration and credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz1 = heapHolder.findClass("org.jeecg.config.JeecgBootConfig");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("org.jeecg.common.constant.CommonConstant");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[JeecgBootConfig] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // Jeecg database properties
            Object clazz2 = heapHolder.findClass("org.jeecg.config.shiro.ShiroConfig");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[JeecgShiroConfig] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
