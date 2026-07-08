package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TencentCloudCredentialSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TencentCloudCredentialSearch.class);


    public String getName() { return "TencentCloudCredentialSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract Tencent Cloud SecretId/SecretKey from SDK and string pool"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    private static final Pattern TC_AK_PATTERN = CredentialPatterns.TENCENT_SECRET_ID;

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // BasicCOSCredentials
            Object clazz1 = heapHolder.findClass("com.tencent.cloud.cos.credentials.BasicCOSCredentials");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.qcloud.cos.auth.BasicCOSCredentials");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[TencentCOSCredentials] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // CredentialProvider (Tencent Cloud SDK v3)
            Object clazz2 = heapHolder.findClass("com.tencentcloudapi.common.Credential");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("secretId", "secretId");
                    put("secretKey", "secretKey");
                    put("token", "token");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[TencentCloudCredential] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // String pool
            try {
                List<String> matches = heapHolder.searchAll(TC_AK_PATTERN);
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (String match : matches) {
                    if (seen.add(match)) {
                        result.append("[TencentAKPattern] ").append(match).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
