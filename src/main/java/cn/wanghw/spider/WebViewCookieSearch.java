package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebViewCookieSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebViewCookieSearch.class);


    public String getName() { return "WebViewCookieSearch"; }
    public String getCategory() { return "http"; }
    public String getDescription() { return "Extract cookies from Java CookieManager and CookieStore instances"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // CookieManager
            Object clazz1 = heapHolder.findClass("java.net.CookieManager");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[CookieManager] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // InMemoryCookieStore
            Object clazz2 = heapHolder.findClass("java.net.InMemoryCookieStore");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[CookieStore] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // HttpCookie
            Object clazz3 = heapHolder.findClass("java.net.HttpCookie");
            if (clazz3 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("name", "name");
                    put("value", "value");
                    put("domain", "domain");
                    put("path", "path");
                    put("secure", "secure");
                    put("httpOnly", "httpOnly");
                }};
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    result.append("[HttpCookie] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
