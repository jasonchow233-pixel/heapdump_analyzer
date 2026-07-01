package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Google OAuth2 client credentials from google-api-java-client
 * com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
 */
public class GoogleOAuth2Search implements ISpider {
    @Override
    public String getName() {
        return "GoogleOAuth2";
    }

    @Override
    public String getCategory() {
        return "auth";
    }

    @Override
    public String getDescription() {
        return "Extract Google OAuth2 client ID and client secret from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets");
            if (clazz != null) {
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    // Try installed/web details
                    String[] detailTypes = {"installed", "web"};
                    for (String dt : detailTypes) {
                        try {
                            Object detail = heapHolder.getFieldValue(instance, dt);
                            if (detail != null) {
                                String clientId = heapHolder.getFieldStringValue(detail, "clientId");
                                String clientSecret = heapHolder.getFieldStringValue(detail, "clientSecret");
                                if (clientId != null) fields.put(dt + ".clientId", clientId);
                                if (clientSecret != null) fields.put(dt + ".clientSecret", clientSecret);
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {}
        return result.toString();
    }
}
