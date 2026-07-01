package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AWSCredentialSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AWSCredentialSearch.class);


    public String getName() { return "AWSCredentialSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract AWS credentials (Access Key/Secret Key) from SDK classes and string pool"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    private static final Pattern AWS_AK_PATTERN = CredentialPatterns.AWS_ACCESS_KEY_ID;
    private static final Pattern AWS_SECRET_PATTERN = CredentialPatterns.AWS_SECRET_ACCESS_KEY;

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // BasicAWSCredentials (SDK v1)
            Object clazz1 = heapHolder.findClass("com.amazonaws.auth.BasicAWSCredentials");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("awsAccessKeyId", "awsAccessKeyId");
                    put("awsSecretKey", "awsSecretKey");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[AWS BasicCredentials] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // AwsBasicCredentials (SDK v2)
            Object clazz2 = heapHolder.findClass("software.amazon.awssdk.auth.credentials.AwsBasicCredentials");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[AWS v2 Credentials] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // AWSCredentialsProviderChain
            Object clazz3 = heapHolder.findClass("com.amazonaws.auth.AWSCredentialsProviderChain");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[AWS CredentialsProvider] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // String pool patterns
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Pattern pattern : new Pattern[]{AWS_AK_PATTERN, AWS_SECRET_PATTERN}) {
                try {
                    List<String> matches = heapHolder.searchStrings(pattern);
                    for (String match : matches) {
                        if (seen.add(match)) {
                            result.append("[AWSKeyPattern] ").append(match).append("\n");
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
