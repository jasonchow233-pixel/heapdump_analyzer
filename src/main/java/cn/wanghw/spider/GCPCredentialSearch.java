package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCPCredentialSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GCPCredentialSearch.class);


    public String getName() { return "GCPCredentialSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract Google Cloud Platform service account credentials and project info"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    private static final Pattern GCP_SA_PATTERN = Pattern.compile("[a-z][a-z0-9-]{4,28}@[a-z][a-z0-9-]{4,28}\\.iam\\.gserviceaccount\\.com");

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ServiceAccountCredentials
            Object clazz1 = heapHolder.findClass("com.google.auth.oauth2.ServiceAccountCredentials");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("clientEmail", "clientEmail");
                    put("projectId", "projectId");
                    put("clientId", "clientId");
                    put("privateKeyId", "privateKeyId");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[GCP ServiceAccount] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // GoogleCredentials
            Object clazz2 = heapHolder.findClass("com.google.auth.oauth2.GoogleCredentials");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[GCP Credentials] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // String pool
            try {
                List<String> matches = heapHolder.searchAll(GCP_SA_PATTERN);
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (String match : matches) {
                    if (seen.add(match)) {
                        result.append("[GCPServiceAccountEmail] ").append(match).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
