package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuoYiConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuoYiConfig.class);


    public String getName() {
        return "RuoYiConfig";
    }

    public String getCategory() {
        return "framework";
    }

    public String getDescription() {
        return "Extract RuoYi framework configuration (database, profiles, etc.)";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.HIGH;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // RuoYiConfig
            Object clazz = heapHolder.findClass("com.ruoyi.common.core.domain.entity.SysConfig");
            if (clazz != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("configKey", "configKey");
                    put("configValue", "configValue");
                    put("configType", "configType");
                }};
                for (Object instance : heapHolder.getInstances(clazz)) {
                    result.append("[SysConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // RuoYi application config
            Object clazz2 = heapHolder.findClass("com.ruoyi.framework.config.RuoYiConfig");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("com.ruoyi.common.config.RuoYiConfig");
            if (clazz2 != null) {
                HashMap<String, String> fieldList2 = new HashMap<String, String>() {{
                    put("name", "name");
                    put("version", "version");
                    put("profile", "profile");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[RuoYiConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList2), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
