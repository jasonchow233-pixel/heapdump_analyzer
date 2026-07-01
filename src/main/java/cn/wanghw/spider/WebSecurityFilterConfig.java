package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSecurityFilterConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityFilterConfig.class);


    public String getName() { return "WebSecurityFilterConfig"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Detect XSS/CSRF security filter configurations and potential bypasses"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // CsrfFilter
            Object clazz1 = heapHolder.findClass("org.springframework.security.web.csrf.CsrfFilter");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[CsrfFilter] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // XssFilter (common custom implementation)
            Object clazz2 = heapHolder.findClass("com.fasterxml.jackson.databind.ObjectMapper");
            // Look for XSS-related filter classes
            Object xssClazz = heapHolder.findClass("org.springframework.web.filter.CharacterEncodingFilter");
            if (xssClazz != null) {
                for (Object instance : heapHolder.getInstances(xssClazz)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[CharacterEncodingFilter] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // HeaderWriterFilter
            Object clazz3 = heapHolder.findClass("org.springframework.security.web.header.HeaderWriterFilter");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HeaderWriterFilter] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // CorsFilter / CorsConfiguration
            Object clazz4 = heapHolder.findClass("org.springframework.web.cors.CorsConfiguration");
            if (clazz4 != null) {
                for (Object instance : heapHolder.getInstances(clazz4)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[CorsConfiguration] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
