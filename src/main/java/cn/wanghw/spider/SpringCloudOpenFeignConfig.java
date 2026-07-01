package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCloudOpenFeignConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudOpenFeignConfig.class);


    public String getName() { return "SpringCloudOpenFeignConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract Spring Cloud OpenFeign client configurations and URLs"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // FeignClientProperties
            Object clazz1 = heapHolder.findClass("org.springframework.cloud.openfeign.FeignClientProperties");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[FeignClientProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // FeignClientConfiguration
            Object clazz2 = heapHolder.findClass("feign.Client$Default");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("feign.httpclient.ApacheHttpClient");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("feign.okhttp.OkHttpClient");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[FeignClient: ").append(heapHolder.getClassName(clazz2)).append("] ")
                            .append(HashMapUtils.dumpString(vals, false));
                }
            }
            // Target.TargetType
            Object clazz3 = heapHolder.findClass("feign.Target$HardCodedTarget");
            if (clazz3 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("type", "type");
                    put("name", "name");
                    put("url", "url");
                }};
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    result.append("[FeignTarget] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
