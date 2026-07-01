package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringBladeConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBladeConfig.class);


    public String getName() { return "SpringBladeConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract SpringBlade microservice framework configuration"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // BladeProperties
            Object clazz1 = heapHolder.findClass("org.springblade.core.launch.constant.LauncherConstant");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("org.springblade.core.launch.props.BladeProperties");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[BladeProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // BladeTokenConfig
            Object clazz2 = heapHolder.findClass("org.springblade.core.secure.props.BladeTokenProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[BladeTokenProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
