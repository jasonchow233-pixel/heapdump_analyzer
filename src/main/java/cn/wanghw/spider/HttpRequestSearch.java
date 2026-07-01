package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Extract HTTP request information from Tomcat connectors.
 * org.apache.coyote.Request, org.apache.catalina.connector.Request
 */
public class HttpRequestSearch implements ISpider {
    @Override
    public String getName() {
        return "HttpRequest";
    }

    @Override
    public String getCategory() {
        return "security";
    }

    @Override
    public String getDescription() {
        return "Extract HTTP request details (URLs, headers, cookies, parameters) from Tomcat request objects";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "org.apache.coyote.Request",
                "org.apache.catalina.connector.RequestFacade"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"requestURI", "queryString", "method",
                            "protocol", "serverName", "remoteAddr", "scheme"};
                    for (String fn : fieldNames) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
                    }
                    // Try to get headers
                    try {
                        Object headers = heapHolder.getFieldValue(instance, "headers");
                        if (headers != null) {
                            HashMap<String, String> headerMap = heapHolder.arrayDump(heapHolder.getMap(headers));
                            if (headerMap != null) {
                                for (Map.Entry<String, String> e : headerMap.entrySet()) {
                                    String key = e.getKey();
                                    if (key.toLowerCase().contains("auth") ||
                                        key.toLowerCase().contains("cookie") ||
                                        key.toLowerCase().contains("token") ||
                                        key.toLowerCase().contains("x-forwarded") ||
                                        key.toLowerCase().contains("x-real")) {
                                        fields.put("header." + key, e.getValue());
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
