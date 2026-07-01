package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkHttpInterceptorSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OkHttpInterceptorSearch.class);


    public String getName() { return "OkHttpInterceptorSearch"; }
    public String getCategory() { return "http"; }
    public String getDescription() { return "Extract OkHttp client interceptors that may contain Authorization headers"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // OkHttpClient
            Object clazz1 = heapHolder.findClass("okhttp3.OkHttpClient");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[OkHttpClient] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // RealCall (contains request with headers)
            Object clazz2 = heapHolder.findClass("okhttp3.internal.http.RealCall");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("okhttp3.RealCall");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[OkHttpCall] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
