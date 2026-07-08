package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuaweiCloudCredentialSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuaweiCloudCredentialSearch.class);


    public String getName() { return "HuaweiCloudCredentialSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract Huawei Cloud AK/SK credentials from SDK classes and string pool"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    private static final Pattern HW_AK_PATTERN = Pattern.compile("HW[F|C]K[A-Za-z0-9]{14,20}");

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // BasicCredentials
            Object clazz1 = heapHolder.findClass("com.huaweicloud.sdk.core.auth.BasicCredentials");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.huaweicloud.sdk.core.auth.GlobalCredentials");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HuaweiCredentials: ").append(heapHolder.getClassName(clazz1)).append("] ")
                            .append(HashMapUtils.dumpString(vals, false));
                }
            }
            // String pool
            try {
                List<String> matches = heapHolder.searchAll(HW_AK_PATTERN);
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (String match : matches) {
                    if (seen.add(match)) {
                        result.append("[HuaweiAKPattern] ").append(match).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
