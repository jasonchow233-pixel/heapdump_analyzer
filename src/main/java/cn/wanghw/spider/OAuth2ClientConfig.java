package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuth2ClientConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2ClientConfig.class);


    public String getName() { return "OAuth2ClientConfig"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract OAuth2/OpenID Connect client registrations and provider credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ClientRegistration (Spring Security OAuth2)
            Object clazz1 = heapHolder.findClass("org.springframework.security.oauth2.client.registration.ClientRegistration");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("registrationId", "registrationId");
                    put("clientId", "clientId");
                    put("clientSecret", "clientSecret");
                    put("clientName", "clientName");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[ClientRegistration] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // ClientRegistration$ProviderDetails
            Object clazz2 = heapHolder.findClass("org.springframework.security.oauth2.client.registration.ClientRegistration$ProviderDetails");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[OAuth2Provider] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // OAuth2AuthorizedClient
            Object clazz3 = heapHolder.findClass("org.springframework.security.oauth2.client.OAuth2AuthorizedClient");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[OAuth2AuthorizedClient] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // Spring Security OAuth2 (legacy) ClientTokenSecret
            Object clazz4 = heapHolder.findClass("org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest");
            if (clazz4 != null) {
                for (Object instance : heapHolder.getInstances(clazz4)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[OAuth2AccessTokenRequest] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
