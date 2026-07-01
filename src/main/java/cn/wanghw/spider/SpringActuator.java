package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringActuator implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringActuator.class);


    public String getName() {
        return "SpringActuator";
    }

    public String getCategory() {
        return "config";
    }

    public String getDescription() {
        return "Extract Spring Boot Actuator environment properties and endpoints";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.MEDIUM;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // EnvironmentEndpoint
            Object clazz = heapHolder.findClass("org.springframework.boot.actuate.env.EnvironmentEndpoint");
            if (clazz != null) {
                for (Object instance : heapHolder.getInstances(clazz)) {
                    result.append("[EnvironmentEndpoint] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
            // EnvironmentEndpointEnvironmentalEntry
            Object clazz2 = heapHolder.findClass("org.springframework.boot.actuate.env.EnvironmentEndpoint$EnvironmentEntryDescriptor");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[EnvEntry] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
