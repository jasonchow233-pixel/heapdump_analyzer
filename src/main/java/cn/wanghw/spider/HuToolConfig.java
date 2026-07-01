package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuToolConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuToolConfig.class);


    public String getName() { return "HuToolConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract Hutool Setting/Props configuration that may contain credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // Setting (Hutool configuration)
            Object clazz1 = heapHolder.findClass("cn.hutool.setting.Setting");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HutoolSetting] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // Props
            Object clazz2 = heapHolder.findClass("cn.hutool.setting.dialect.Props");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HutoolProps] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
