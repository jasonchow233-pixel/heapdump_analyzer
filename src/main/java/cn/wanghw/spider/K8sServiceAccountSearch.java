package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sServiceAccountSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(K8sServiceAccountSearch.class);


    public String getName() { return "K8sServiceAccountSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract Kubernetes ServiceAccount tokens and cluster connection info"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    private static final Pattern K8S_TOKEN_PATTERN = Pattern.compile("eyJhbGciOiJSUzI1NiIs[A-Za-z0-9_\\-]+\\.eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+");

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ClientBuilder
            Object clazz1 = heapHolder.findClass("io.kubernetes.client.util.ClientBuilder");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[K8sClientBuilder] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // KubeConfig
            Object clazz2 = heapHolder.findClass("io.kubernetes.client.util.KubeConfig");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    String server = heapHolder.getFieldStringValue(instance, "server");
                    String token = heapHolder.getFieldStringValue(instance, "accessToken");
                    result.append("[KubeConfig] server=").append(server);
                    if (token != null && !token.isEmpty()) {
                        result.append(", token=").append(token.length() > 30 ? token.substring(0, 30) + "..." : token);
                    }
                    result.append("\n");
                }
            }
            // ApiClient
            Object clazz3 = heapHolder.findClass("io.kubernetes.client.openapi.ApiClient");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    String basePath = heapHolder.getFieldStringValue(instance, "basePath");
                    result.append("[K8sApiClient] basePath=").append(basePath).append("\n");
                }
            }
            // String pool K8s token pattern
            try {
                List<String> matches = heapHolder.searchStrings(K8S_TOKEN_PATTERN);
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (String match : matches) {
                    if (seen.add(match)) {
                        String display = match.length() > 100 ? match.substring(0, 100) + "..." : match;
                        result.append("[K8sTokenPattern] ").append(display).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
