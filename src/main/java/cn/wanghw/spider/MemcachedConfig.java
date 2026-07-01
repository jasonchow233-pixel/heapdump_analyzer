package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemcachedConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemcachedConfig.class);


    public String getName() { return "MemcachedConfig"; }
    public String getCategory() { return "cache"; }
    public String getDescription() { return "Extract Memcached connection configuration"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // Xmemcached
            Object clazz1 = heapHolder.findClass("net.rubyeye.xmemcached.XMemcachedClient");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("net.rubyeye.xmemcached.MemcachedClient");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[XMemcachedClient] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
            // Spring Memcached
            Object clazz2 = heapHolder.findClass("org.springframework.boot.autoconfigure.cache.CacheProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[CacheProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
