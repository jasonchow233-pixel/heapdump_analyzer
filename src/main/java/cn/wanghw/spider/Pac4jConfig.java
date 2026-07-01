package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pac4jConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Pac4jConfig.class);


    public String getName() { return "Pac4jConfig"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract Pac4j security client configurations (OAuth/SAML/OIDC/CAS)"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // OAuthConfiguration / Config
            Object clazz1 = heapHolder.findClass("org.pac4j.core.config.Config");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[Pac4jConfig] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // OAuth20Client
            Object clazz2 = heapHolder.findClass("org.pac4j.oauth.client.OAuth20Client");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    String name = heapHolder.getFieldStringValue(instance, "name");
                    String callbackUrl = heapHolder.getFieldStringValue(instance, "callbackUrl");
                    result.append("[OAuth20Client] name=").append(name)
                            .append(", callbackUrl=").append(callbackUrl).append("\n");
                }
            }
            // OidcClient
            Object clazz3 = heapHolder.findClass("org.pac4j.oidc.client.OidcClient");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    String name = heapHolder.getFieldStringValue(instance, "name");
                    result.append("[OidcClient] name=").append(name).append("\n");
                }
            }
            // SAML2Client
            Object clazz4 = heapHolder.findClass("org.pac4j.saml.client.SAML2Client");
            if (clazz4 != null) {
                for (Object instance : heapHolder.getInstances(clazz4)) {
                    String name = heapHolder.getFieldStringValue(instance, "name");
                    result.append("[SAML2Client] name=").append(name).append("\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
