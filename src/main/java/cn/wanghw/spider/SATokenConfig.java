package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SATokenConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SATokenConfig.class);


    public String getName() { return "SATokenConfig"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract SA-Token configuration (token name, timeout, activity settings)"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // SaTokenConfig
            Object clazz1 = heapHolder.findClass("cn.dev33.satoken.config.SaTokenConfig");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("tokenName", "tokenName");
                    put("timeout", "timeout");
                    put("activityTimeout", "activityTimeout");
                    put("isShare", "isShare");
                    put("isReadCookie", "isReadCookie");
                    put("isReadHeader", "isReadHeader");
                    put("isConcurrent", "isConcurrent");
                    put("tokenStyle", "tokenStyle");
                    put("tokenPrefix", "tokenPrefix");
                    put("jwtSecretKey", "jwtSecretKey");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[SaTokenConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // SaTokenDao (token storage)
            Object clazz2 = heapHolder.findClass("cn.dev33.satoken.dao.SaTokenDao");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[SaTokenDao] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
