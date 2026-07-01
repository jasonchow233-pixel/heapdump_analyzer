package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Auth0 credentials from auth0-java SDK
 * com.auth0.client.auth.AuthAPI
 */
public class Auth0ConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "Auth0Config";
    }

    @Override
    public String getCategory() {
        return "auth";
    }

    @Override
    public String getDescription() {
        return "Extract Auth0 authentication configuration (domain, client ID, client secret) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("com.auth0.client.auth.AuthAPI");
            if (clazz != null) {
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"domain", "clientId", "clientSecret"};
                    for (String fn : fieldNames) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
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
