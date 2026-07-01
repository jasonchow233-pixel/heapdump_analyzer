package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract session information from heap: HttpSession, Spring Session, Redis Session, etc.
 * Sessions may contain authenticated user info, tokens, and sensitive attributes.
 */
public class SessionSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionSearch.class);


    public String getName() { return "SessionSearch"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract HTTP sessions, Spring Sessions, and session attributes with auth info"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    // Keys likely to contain auth info in session attributes
    private static final String[] AUTH_KEYS = {
            "SPRING_SECURITY_CONTEXT", "user", "username", "userId", "loginUser",
            "currentUser", "auth", "token", "accessToken", "sessionId",
            "shiroSecurityContext", "org.apache.shiro.subject",
            "cas_assertion", "_const_cas_assertion_",
            "samlUser", "authenticatedUser", "loginInfo",
            "admin", "role", "permissions", "authorities"
    };

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // 1. Standard HttpSession (Tomcat)
            sniffHttpSession(heapHolder, result);
            // 2. Spring Session
            sniffSpringSession(heapHolder, result);
            // 3. Shiro Session
            sniffShiroSession(heapHolder, result);
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }

    private void sniffHttpSession(IHeapHolder heapHolder, StringBuilder result) {
        // Tomcat StandardSession
        Object clazz = heapHolder.findClass("org.apache.catalina.session.StandardSession");
        if (clazz == null)
            clazz = heapHolder.findClass("org.apache.catalina.session.StandardSessionFacade");
        if (clazz == null)
            clazz = heapHolder.findClass("org.eclipse.jetty.server.session.AbstractSession");
        if (clazz == null)
            clazz = heapHolder.findClass("io.undertow.server.session.SessionImpl");

        if (clazz != null) {
            HashMap<String, String> fieldList = new HashMap<String, String>() {{
                put("id", "id");
                put("creationTime", "creationTime");
                put("lastAccessedTime", "lastAccessedTime");
                put("maxInactiveInterval", "maxInactiveInterval");
                put("principal", "principal");
                put("authType", "authType");
            }};
            for (Object instance : heapHolder.getInstances(clazz)) {
                HashMap<String, String> values = heapHolder.getFieldsByNameList(instance, fieldList);
                // Try to read session attributes (Map)
                Object attributes = heapHolder.getFieldValue(instance, "attributes");
                if (attributes != null && heapHolder.isMap(attributes)) {
                    HashMap<String, String> attrMap = heapHolder.arrayDump(heapHolder.getMap(attributes));
                    String attrDump = HashMapUtils.dumpString(attrMap, false);
                    if (attrDump != null && !attrDump.isEmpty()) {
                        values.put("attributes", filterAuthAttributes(attrDump));
                    }
                }
                result.append("[HttpSession] ").append(HashMapUtils.dumpString(values, false));
            }
        }
    }

    private void sniffSpringSession(IHeapHolder heapHolder, StringBuilder result) {
        // Spring Session MapSession
        Object clazz1 = heapHolder.findClass("org.springframework.session.MapSession");
        if (clazz1 != null) {
            for (Object instance : heapHolder.getInstances(clazz1)) {
                String id = heapHolder.getFieldStringValue(instance, "id");
                Object attrs = heapHolder.getFieldValue(instance, "sessionAttributes");
                String attrDump = "";
                if (attrs != null && heapHolder.isMap(attrs)) {
                    HashMap<String, String> attrMap = heapHolder.arrayDump(heapHolder.getMap(attrs));
                    attrDump = HashMapUtils.dumpString(attrMap, false);
                }
                result.append("[SpringSession] id=").append(id);
                if (!attrDump.isEmpty()) {
                    result.append(", attributes=").append(filterAuthAttributes(attrDump));
                }
                result.append("\n");
            }
        }

        // Redis session
        Object clazz2 = heapHolder.findClass("org.springframework.session.data.redis.RedisSession");
        if (clazz2 == null)
            clazz2 = heapHolder.findClass("org.springframework.session.data.redis.RedisIndexedSessionRepository$RedisSession");
        if (clazz2 != null) {
            for (Object instance : heapHolder.getInstances(clazz2)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[RedisSession] ").append(HashMapUtils.dumpString(vals, false));
            }
        }

        // JDBC session
        Object clazz3 = heapHolder.findClass("org.springframework.session.jdbc.JdbcIndexedSessionRepository$JdbcSession");
        if (clazz3 != null) {
            for (Object instance : heapHolder.getInstances(clazz3)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[JdbcSession] ").append(HashMapUtils.dumpString(vals, false));
            }
        }
    }

    private void sniffShiroSession(IHeapHolder heapHolder, StringBuilder result) {
        Object clazz = heapHolder.findClass("org.apache.shiro.session.mgt.SimpleSession");
        if (clazz != null) {
            HashMap<String, String> fieldList = new HashMap<String, String>() {{
                put("id", "id");
                put("host", "host");
                put("userId", "userId");
                put("startTimestamp", "startTimestamp");
                put("lastAccessTime", "lastAccessTime");
            }};
            for (Object instance : heapHolder.getInstances(clazz)) {
                HashMap<String, String> values = heapHolder.getFieldsByNameList(instance, fieldList);
                Object attrs = heapHolder.getFieldValue(instance, "attributes");
                if (attrs != null && heapHolder.isMap(attrs)) {
                    HashMap<String, String> attrMap = heapHolder.arrayDump(heapHolder.getMap(attrs));
                    String attrDump = HashMapUtils.dumpString(attrMap, false);
                    if (attrDump != null && !attrDump.isEmpty()) {
                        values.put("attributes", filterAuthAttributes(attrDump));
                    }
                }
                result.append("[ShiroSession] ").append(HashMapUtils.dumpString(values, false));
            }
        }

        // DelegatingSubject (Shiro authenticated subject)
        Object clazz2 = heapHolder.findClass("org.apache.shiro.subject.support.DelegatingSubject");
        if (clazz2 != null) {
            for (Object instance : heapHolder.getInstances(clazz2)) {
                Object principals = heapHolder.getFieldValue(instance, "principals");
                boolean authenticated = Boolean.TRUE.equals(heapHolder.getFieldValue(instance, "authenticated"));
                if (authenticated && principals != null) {
                    result.append("[ShiroSubject] principals=").append(principals.toString())
                            .append(", authenticated=true\n");
                }
            }
        }
    }

    /**
     * Filter session attributes to only show auth-related keys (reduce noise)
     */
    private String filterAuthAttributes(String allAttributes) {
        if (allAttributes == null || allAttributes.isEmpty()) return "";
        StringBuilder filtered = new StringBuilder();
        for (String line : allAttributes.split("\n")) {
            String lowerLine = line.toLowerCase();
            for (String key : AUTH_KEYS) {
                if (lowerLine.contains(key.toLowerCase())) {
                    filtered.append(line).append("\n");
                    break;
                }
            }
        }
        // If no auth keys found, show first 3 attributes as sample
        if (filtered.length() == 0) {
            String[] lines = allAttributes.split("\n");
            int limit = Math.min(3, lines.length);
            for (int i = 0; i < limit; i++) {
                filtered.append(lines[i]).append("\n");
            }
            if (lines.length > 3) {
                filtered.append("... (").append(lines.length - 3).append(" more attributes)\n");
            }
        }
        return filtered.toString().trim();
    }
}
