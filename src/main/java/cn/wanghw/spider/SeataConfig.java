package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeataConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataConfig.class);


    public String getName() { return "SeataConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract Seata distributed transaction configuration and registry info"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // SeataProperties
            Object clazz1 = heapHolder.findClass("io.seata.spring.boot.autoconfigure.SeataProperties");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("txServiceGroup", "txServiceGroup");
                    put("applicationId", "applicationId");
                    put("enableAutoDataSourceProxyMode", "enableAutoDataSourceProxyMode");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[SeataProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // RegistryConfig
            Object clazz2 = heapHolder.findClass("io.seata.config.ConfigurationFactory");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[SeataConfigurationFactory] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
