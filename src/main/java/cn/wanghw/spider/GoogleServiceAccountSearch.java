package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Google Cloud ServiceAccountCredentials from google-auth-library-java
 * com.google.auth.oauth2.ServiceAccountCredentials
 */
public class GoogleServiceAccountSearch implements ISpider {
    @Override
    public String getName() {
        return "GoogleServiceAccount";
    }

    @Override
    public String getCategory() {
        return "cloud";
    }

    @Override
    public String getDescription() {
        return "Extract Google Cloud Service Account credentials (client email, project ID, private key ID) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "com.google.auth.oauth2.ServiceAccountCredentials",
                "com.google.api.client.googleapis.auth.oauth2.GoogleCredential",
                "com.google.api.client.googleapis.auth.oauth2.ServiceAccountCredential"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"clientId", "clientEmail", "privateKeyId",
                            "projectId", "transportName", "tokenServerUri"};
                    for (String fn : fieldNames) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
                    }
                    // Try to get scopes
                    Object scopes = heapHolder.getFieldValue(instance, "scopes");
                    if (scopes != null) {
                        fields.put("scopes", heapHolder.toString(scopes));
                    }
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
