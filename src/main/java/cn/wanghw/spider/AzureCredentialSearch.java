package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureCredentialSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialSearch.class);


    public String getName() { return "AzureCredentialSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract Azure cloud credentials (client ID/secret, connection strings)"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    private static final Pattern AZURE_CONN_PATTERN = Pattern.compile("Endpoint=sb://[^;]+;SharedAccessKeyName=[^;]+;SharedAccessKey=[^;]+");
    private static final Pattern AZURE_STORAGE_PATTERN = Pattern.compile("DefaultEndpointsProtocol=https?;AccountName=[^;]+;AccountKey=[A-Za-z0-9+/=]{40,}");

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ClientSecretCredential
            Object clazz1 = heapHolder.findClass("com.azure.identity.ClientSecretCredential");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("clientId", "clientId");
                    put("clientSecret", "clientSecret");
                    put("tenantId", "tenantId");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[AzureClientSecret] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // ApplicationTokenCredentials (older SDK)
            Object clazz2 = heapHolder.findClass("com.microsoft.azure.credentials.ApplicationTokenCredentials");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[AzureAppToken] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // String pool patterns
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Pattern pattern : new Pattern[]{AZURE_CONN_PATTERN, AZURE_STORAGE_PATTERN}) {
                try {
                    List<String> matches = heapHolder.searchAll(pattern);
                    for (String match : matches) {
                        if (seen.add(match)) {
                            result.append("[AzureConnectionString] ").append(match).append("\n");
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
