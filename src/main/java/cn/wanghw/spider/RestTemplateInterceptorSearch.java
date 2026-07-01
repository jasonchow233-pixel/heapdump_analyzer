package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestTemplateInterceptorSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateInterceptorSearch.class);


    public String getName() { return "RestTemplateInterceptorSearch"; }
    public String getCategory() { return "http"; }
    public String getDescription() { return "Extract Spring RestTemplate interceptors containing auth headers and tokens"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.MEDIUM; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // RestTemplate
            Object clazz1 = heapHolder.findClass("org.springframework.web.client.RestTemplate");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[RestTemplate] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // BasicAuthorizationInterceptor
            Object clazz2 = heapHolder.findClass("org.springframework.http.client.support.BasicAuthenticationInterceptor");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("org.springframework.http.client.support.BasicAuthorizationInterceptor");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    String username = heapHolder.getFieldStringValue(instance, "username");
                    String password = heapHolder.getFieldStringValue(instance, "password");
                    result.append("[BasicAuthInterceptor] username=").append(username)
                            .append(", password=").append(password).append("\n");
                }
            }
            // OAuth2AuthorizedClientManager (token-based)
            Object clazz3 = heapHolder.findClass("org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[OAuth2ClientManager] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
