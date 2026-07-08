package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliyunCredentialSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunCredentialSearch.class);


    public String getName() { return "AliyunCredentialSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract Aliyun/Alibaba Cloud AccessKey and SecretKey from SDK and string pool"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    private static final Pattern ALIYUN_AK_PATTERN = CredentialPatterns.ALIYUN_ACCESS_KEY_ID;

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // AlibabaCloudCredentials
            Object clazz1 = heapHolder.findClass("com.aliyuncs.auth.AlibabaCloudCredentials");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("com.aliyuncs.auth.BasicCredentials");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[AliyunCredentials] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // DefaultProfile
            Object clazz2 = heapHolder.findClass("com.aliyuncs.DefaultProfile");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("accessKeyId", "accessKeyId");
                    put("accessKeySecret", "accessKeySecret");
                    put("regionId", "regionId");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[AliyunProfile] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // OSS credentials
            Object clazz3 = heapHolder.findClass("com.aliyun.oss.common.auth.DefaultCredentialProvider");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[AliyunOSSCredential] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // String pool LTAI pattern
            try {
                List<String> matches = heapHolder.searchAll(ALIYUN_AK_PATTERN);
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (String match : matches) {
                    if (seen.add(match)) {
                        result.append("[AliyunAKPattern] ").append(match).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
