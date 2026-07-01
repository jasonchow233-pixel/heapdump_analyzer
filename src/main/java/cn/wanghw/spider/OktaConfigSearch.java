package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Okta credentials from okta-sdk-java / okta-spring-boot
 * com.okta.sdk.authc.credentials.TokenCredentials, com.okta.spring.boot.oauth.OktaOAuth2Properties
 */
public class OktaConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "OktaConfig";
    }

    @Override
    public String getCategory() {
        return "auth";
    }

    @Override
    public String getDescription() {
        return "Extract Okta authentication configuration (domain, client ID, API token) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "com.okta.sdk.authc.credentials.TokenCredentials",
                "com.okta.spring.boot.oauth.OktaOAuth2Properties",
                "com.okta.sdk.impl.client.DefaultClient"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"apiToken", "clientId", "clientSecret",
                            "orgUrl", "baseUrl", "domain"};
                    for (String fn : fieldNames) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
                    }
                    if (!fields.isEmpty()) {
                        result.append(className).append(": ")
                              .append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
