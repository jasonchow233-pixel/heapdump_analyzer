package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Extract all URLs from java.net.URL instances in heap.
 * Useful for finding internal service endpoints, API gateways, admin panels.
 */
public class URLSearch implements ISpider {
    @Override
    public String getName() {
        return "URLSearch";
    }

    @Override
    public String getCategory() {
        return "security";
    }

    @Override
    public String getDescription() {
        return "Extract all URL instances from heap to find internal service endpoints and API URLs";
    }

    private static final Pattern INTERESTING_URL = Pattern.compile(
            "(api[_-]?key|secret|token|admin|internal|private|debug|swagger|actuator|" +
            "consul|eureka|nacos|vault|grafana|prometheus|kibana|elasticsearch|" +
            "\\.firebaseio\\.com|firestore\\.googleapis|s3\\.amazonaws|" +
            "rds\\.amazonaws| dynamodb|lambda\\.us-|cloudfunctions\\.net)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("java.net.URL");
            if (clazz == null) return "";
            for (Object instance : heapHolder.getInstances(clazz)) {
                try {
                    String protocol = heapHolder.getFieldStringValue(instance, "protocol");
                    String host = heapHolder.getFieldStringValue(instance, "host");
                    String path = heapHolder.getFieldStringValue(instance, "path");
                    String query = heapHolder.getFieldStringValue(instance, "query");
                    if (host == null || host.isEmpty()) continue;
                    // Only include URLs with interesting patterns
                    String fullUrl = (protocol != null ? protocol + "://" : "") + host +
                                     (path != null ? path : "") +
                                     (query != null ? "?" + query : "");
                    if (INTERESTING_URL.matcher(fullUrl).find() ||
                        host.endsWith(".internal") ||
                        host.endsWith(".local") ||
                        host.contains("10.") ||
                        host.contains("192.168.") ||
                        host.startsWith("172.16.") ||
                        host.startsWith("172.17.") ||
                        host.startsWith("172.18.") ||
                        host.startsWith("172.19.") ||
                        host.startsWith("172.2") ||
                        host.startsWith("172.3")) {
                        HashMap<String, String> fields = new HashMap<>();
                        fields.put("url", fullUrl);
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return result.toString();
    }
}
