package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 全面搜索HTTP请求相关信息，包括：
 * - HTTP Request (Tomcat, Jetty, Spring, OkHttp, Apache HttpClient)
 * - HTTP Response
 * - URL/URI objects
 * - HTTP Client configurations
 * - Request/Response headers, body, parameters
 */
public class HttpFullRequestSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpFullRequestSearch.class);

    @Override
    public String getName() {
        return "HttpFullRequest";
    }

    @Override
    public String getCategory() {
        return "network";
    }

    @Override
    public String getDescription() {
        return "Extract complete HTTP request/response data (URL, method, headers, payload, cookies, response) from various HTTP frameworks";
    }

    @Override
    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.HIGH;
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        List<String> sections = new ArrayList<>();

        // 1. Tomcat Request/Response
        sections.add(sniffTomcatRequest(heapHolder));

        // 2. Spring Request
        sections.add(sniffSpringRequest(heapHolder));

        // 3. OkHttp Request/Response
        sections.add(sniffOkHttp(heapHolder));

        // 4. Apache HttpClient
        sections.add(sniffApacheHttpClient(heapHolder));

        // 5. URL objects
        sections.add(sniffURLs(heapHolder));

        // 6. URI objects
        sections.add(sniffURIs(heapHolder));

        // 7. HTTP Connection pools
        sections.add(sniffConnectionPools(heapHolder));

        for (String section : sections) {
            if (section != null && !section.isEmpty()) {
                result.append(section).append("\n");
            }
        }

        return result.length() == 0 ? null : result.toString();
    }

    private String sniffTomcatRequest(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "org.apache.coyote.Request",
            "org.apache.catalina.connector.RequestFacade",
            "org.apache.catalina.connector.Request"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();

                    // Basic request info
                    String[] basicFields = {"requestURI", "queryString", "method",
                                          "protocol", "serverName", "remoteAddr", "scheme"};
                    for (String fn : basicFields) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
                    }

                    // Headers
                    try {
                        Object headers = heapHolder.getFieldValue(instance, "headers");
                        if (headers != null) {
                            HashMap<String, String> headerMap = heapHolder.arrayDump(heapHolder.getMap(headers));
                            if (headerMap != null) {
                                for (Map.Entry<String, String> e : headerMap.entrySet()) {
                                    fields.put("header." + e.getKey(), e.getValue());
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    // Parameters
                    try {
                        Object parameters = heapHolder.getFieldValue(instance, "parameters");
                        if (parameters != null) {
                            HashMap<String, String> paramMap = heapHolder.arrayDump(heapHolder.getMap(parameters));
                            if (paramMap != null) {
                                for (Map.Entry<String, String> e : paramMap.entrySet()) {
                                    fields.put("param." + e.getKey(), e.getValue());
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    // Cookies
                    try {
                        Object cookies = heapHolder.getFieldValue(instance, "cookies");
                        if (cookies != null) {
                            fields.put("cookies", heapHolder.toString(cookies));
                        }
                    } catch (Exception ignored) {}

                    if (!fields.isEmpty()) {
                        result.append("[TomcatRequest] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to sniff {}: {}", className, e.getMessage());
            }
        }

        return result.toString();
    }

    private String sniffSpringRequest(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();

        // Spring's ContentCachingRequestWrapper
        String[] classNames = {
            "org.springframework.web.util.ContentCachingRequestWrapper",
            "org.springframework.web.context.request.ServletRequestAttributes"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    fields.put("class", className);

                    // Try to get cached content
                    try {
                        Object content = heapHolder.getFieldValue(instance, "content");
                        if (content != null) {
                            byte[] contentBytes = heapHolder.toByteArray(content);
                            if (contentBytes != null && contentBytes.length > 0) {
                                fields.put("payload", new String(contentBytes));
                            }
                        }
                    } catch (Exception ignored) {}

                    result.append("[SpringRequest] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }

    private String sniffOkHttp(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "okhttp3.Request",
            "okhttp3.Response",
            "okhttp3.internal.connection.RealConnection"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();

                    if (className.contains("Request")) {
                        // Request
                        try {
                            Object url = heapHolder.getFieldValue(instance, "url");
                            if (url != null) {
                                fields.put("url", heapHolder.toString(url));
                            }
                        } catch (Exception ignored) {}

                        String method = heapHolder.getFieldStringValue(instance, "method");
                        if (method != null) {
                            fields.put("method", method);
                        }

                        // Headers
                        try {
                            Object headers = heapHolder.getFieldValue(instance, "headers");
                            if (headers != null) {
                                fields.put("headers", heapHolder.toString(headers));
                            }
                        } catch (Exception ignored) {}

                        // Body
                        try {
                            Object body = heapHolder.getFieldValue(instance, "body");
                            if (body != null) {
                                fields.put("body", "present");
                            }
                        } catch (Exception ignored) {}

                        result.append("[OkHttpRequest] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                    } else if (className.contains("Response")) {
                        // Response
                        try {
                            Object request = heapHolder.getFieldValue(instance, "request");
                            if (request != null) {
                                Object url = heapHolder.getFieldValue(request, "url");
                                if (url != null) {
                                    fields.put("url", heapHolder.toString(url));
                                }
                            }
                        } catch (Exception ignored) {}

                        Integer code = null;
                        try {
                            Object codeObj = heapHolder.getFieldValue(instance, "code");
                            if (codeObj != null) {
                                fields.put("statusCode", codeObj.toString());
                            }
                        } catch (Exception ignored) {}

                        String message = heapHolder.getFieldStringValue(instance, "message");
                        if (message != null) {
                            fields.put("statusMessage", message);
                        }

                        result.append("[OkHttpResponse] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }

    private String sniffApacheHttpClient(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "org.apache.http.impl.client.CloseableHttpClient",
            "org.apache.http.client.methods.HttpRequestBase",
            "org.apache.http.client.methods.HttpGet",
            "org.apache.http.client.methods.HttpPost",
            "org.apache.http.client.methods.HttpPut",
            "org.apache.http.client.methods.HttpDelete"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    fields.put("clientClass", className);

                    // URI
                    try {
                        Object uri = heapHolder.getFieldValue(instance, "uri");
                        if (uri != null) {
                            fields.put("uri", heapHolder.toString(uri));
                        }
                    } catch (Exception ignored) {}

                    // Method
                    String method = heapHolder.getFieldStringValue(instance, "method");
                    if (method != null) {
                        fields.put("method", method);
                    }

                    // Headers
                    try {
                        Object headerGroup = heapHolder.getFieldValue(instance, "headergroup");
                        if (headerGroup != null) {
                            fields.put("headers", heapHolder.toString(headerGroup));
                        }
                    } catch (Exception ignored) {}

                    // Config
                    try {
                        Object config = heapHolder.getFieldValue(instance, "config");
                        if (config != null) {
                            HashMap<String, String> configMap = heapHolder.getAllFieldValues(config);
                            if (configMap != null && !configMap.isEmpty()) {
                                fields.put("config", HashMapUtils.dumpString(configMap, false));
                            }
                        }
                    } catch (Exception ignored) {}

                    result.append("[ApacheHttpClient] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }

    private String sniffURLs(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("java.net.URL");
            if (clazz == null) return result.toString();

            int count = 0;
            for (Object instance : heapHolder.getInstances(clazz)) {
                if (count++ > 100) break;  // Limit to 100 URLs

                try {
                    String protocol = heapHolder.getFieldStringValue(instance, "protocol");
                    String host = heapHolder.getFieldStringValue(instance, "host");
                    String path = heapHolder.getFieldStringValue(instance, "path");
                    String query = heapHolder.getFieldStringValue(instance, "query");

                    if (host != null && !host.isEmpty()) {
                        HashMap<String, String> fields = new HashMap<>();
                        String fullUrl = (protocol != null ? protocol + "://" : "") + host +
                                        (path != null ? path : "") +
                                        (query != null ? "?" + query : "");
                        fields.put("url", fullUrl);

                        // Port
                        try {
                            Object port = heapHolder.getFieldValue(instance, "port");
                            if (port != null) {
                                fields.put("port", port.toString());
                            }
                        } catch (Exception ignored) {}

                        result.append("[URL] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to sniff URLs: {}", e.getMessage());
        }

        return result.toString();
    }

    private String sniffURIs(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("java.net.URI");
            if (clazz == null) return result.toString();

            int count = 0;
            for (Object instance : heapHolder.getInstances(clazz)) {
                if (count++ > 100) break;  // Limit to 100 URIs

                try {
                    String scheme = heapHolder.getFieldStringValue(instance, "scheme");
                    String host = heapHolder.getFieldStringValue(instance, "host");
                    String path = heapHolder.getFieldStringValue(instance, "path");
                    String query = heapHolder.getFieldStringValue(instance, "query");
                    String fragment = heapHolder.getFieldStringValue(instance, "fragment");

                    if (host != null && !host.isEmpty()) {
                        HashMap<String, String> fields = new HashMap<>();
                        String fullUri = (scheme != null ? scheme + "://" : "") + host +
                                        (path != null ? path : "") +
                                        (query != null ? "?" + query : "") +
                                        (fragment != null ? "#" + fragment : "");
                        fields.put("uri", fullUri);

                        result.append("[URI] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to sniff URIs: {}", e.getMessage());
        }

        return result.toString();
    }

    private String sniffConnectionPools(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "org.apache.http.impl.conn.PoolingHttpClientConnectionManager",
            "okhttp3.ConnectionPool",
            "org.apache.http.pool.AbstractConnPool"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();

                    // Pool stats
                    try {
                        Object totalStats = heapHolder.getFieldValue(instance, "totalStats");
                        if (totalStats != null) {
                            HashMap<String, String> statsMap = heapHolder.getAllFieldValues(totalStats);
                            if (statsMap != null) {
                                fields.putAll(statsMap);
                            }
                        }
                    } catch (Exception ignored) {}

                    if (!fields.isEmpty()) {
                        result.append("[ConnectionPool] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }
}