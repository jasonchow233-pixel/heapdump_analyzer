package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PigMicroserviceConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PigMicroserviceConfig.class);


    public String getName() { return "PigMicroserviceConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract Pig4Cloud microservice platform configuration"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // PigProperties
            Object clazz1 = heapHolder.findClass("com.pig4cloud.pig.common.core.constant.CommonConstants");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.pig4cloud.pig.common.core.util.WebUtils");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[PigConstants] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // PigAuthSecurityConfigProperties
            Object clazz2 = heapHolder.findClass("com.pig4cloud.pig.auth.config.PigSecurityProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[PigSecurityProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
