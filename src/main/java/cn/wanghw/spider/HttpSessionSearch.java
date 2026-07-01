package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Extract HTTP session information from Tomcat/Servlet containers.
 * Filters for active sessions with authentication data.
 * org.apache.catalina.session.StandardSession
 */
public class HttpSessionSearch implements ISpider {
    @Override
    public String getName() {
        return "HttpSession";
    }

    @Override
    public String getCategory() {
        return "auth";
    }

    @Override
    public String getDescription() {
        return "Extract HTTP session data (session ID, attributes, auth info) from Tomcat/Servlet containers";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "org.apache.catalina.session.StandardSession",
                "org.apache.catalina.session.ManagerBase",
                "org.springframework.session.MapSession"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String id = heapHolder.getFieldStringValue(instance, "id");
                    if (id != null && !id.isEmpty()) {
                        fields.put("sessionId", id);
                    }
                    String authType = heapHolder.getFieldStringValue(instance, "authType");
                    if (authType != null && !authType.isEmpty()) {
                        fields.put("authType", authType);
                    }
                    // Check creation time and last accessed
                    Object creationTime = heapHolder.getFieldValue(instance, "creationTime");
                    if (creationTime != null) {
                        fields.put("creationTime", creationTime.toString());
                    }
                    Object lastAccessedTime = heapHolder.getFieldValue(instance, "lastAccessedTime");
                    if (lastAccessedTime != null) {
                        fields.put("lastAccessedTime", lastAccessedTime.toString());
                    }
                    Object principal = heapHolder.getFieldValue(instance, "principal");
                    if (principal != null) {
                        String principalName = heapHolder.getFieldStringValue(principal, "name");
                        if (principalName != null) {
                            fields.put("principal", principalName);
                        }
                    }
                    // Check attributes map for auth-related data
                    Object attributes = heapHolder.getFieldValue(instance, "attributes");
                    if (attributes != null) {
                        HashMap<String, String> attrMap = heapHolder.arrayDump(heapHolder.getMap(attributes));
                        if (attrMap != null) {
                            for (Map.Entry<String, String> e : attrMap.entrySet()) {
                                String key = e.getKey();
                                if (key.toLowerCase().contains("auth") ||
                                    key.toLowerCase().contains("user") ||
                                    key.toLowerCase().contains("login") ||
                                    key.toLowerCase().contains("token") ||
                                    key.toLowerCase().contains("csrf") ||
                                    key.toLowerCase().contains("session")) {
                                    fields.put("attr." + key, e.getValue());
                                }
                            }
                        }
                    }
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
