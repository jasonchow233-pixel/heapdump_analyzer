package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringSecurityConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringSecurityConfig.class);


    public String getName() { return "SpringSecurityConfig"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract Spring Security filter chain configuration and access rules"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // FilterChainProxy - contains all security filter chains
            Object clazz1 = heapHolder.findClass("org.springframework.security.web.FilterChainProxy");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[FilterChainProxy] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // SecurityProperties
            Object clazz2 = heapHolder.findClass("org.springframework.boot.autoconfigure.security.SecurityProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[SecurityProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // HttpSecurity configuration
            Object clazz3 = heapHolder.findClass("org.springframework.security.config.annotation.web.builders.HttpSecurity");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HttpSecurity] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // WebSecurityConfigurerAdapter (deprecated but still widely used)
            Object clazz4 = heapHolder.findClass("org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter");
            if (clazz4 != null) {
                for (Object instance : heapHolder.getInstances(clazz4)) {
                    String className = heapHolder.getClassName(clazz4);
                    result.append("[WebSecurityConfigurer] class=").append(className).append("\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
