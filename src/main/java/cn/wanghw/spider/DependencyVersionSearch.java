package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract dependency versions from heap and identify known vulnerable versions.
 * Scans common library classes for version strings.
 */
public class DependencyVersionSearch implements ISpider {
    @Override
    public String getName() {
        return "DependencyVersion";
    }

    @Override
    public String getCategory() {
        return "security";
    }

    @Override
    public String getDescription() {
        return "Extract dependency versions from heap and flag known vulnerable libraries";
    }

    private static final String[][] VERSION_CLASSES = {
            {"org.springframework.core.SpringVersion", "version", "Spring Framework"},
            {"org.springframework.boot.SpringBootVersion", "version", "Spring Boot"},
            {"org.apache.tomcat.util.http.ServerCookie", "STRICT_SERVLET_COMPLIANCE", "Tomcat"},
            {"org.apache.commons.lang3.StringUtils", "COMMA", "Commons Lang3"},
            {"io.netty.util.Version", "artifactId", "Netty"},
            {"org.apache.kafka.common.utils.AppInfoParser", "VERSION", "Kafka Client"},
            {"com.mongodb.internal.build.MongoDriverVersion", "VERSION", "MongoDB Driver"},
            {"redis.clients.jedis.Jedis", "DEFAULT_TIMEOUT", "Jedis"},
            {"com.rabbitmq.client.LongString", "DEFAULT_CHARSET", "RabbitMQ Client"},
            {"org.elasticsearch.Version", "id", "Elasticsearch"},
    };

    // Known vulnerable version patterns (simplified)
    private static final String[][] VULN_PATTERNS = {
            {"Spring Framework", "4\\..*|5\\.[0-2]\\..*|5\\.3\\.[0-9]\\b", "CVE-2022-22965 (Spring4Shell) or older"},
            {"Spring Boot", "2\\.[0-6]\\..*", "Multiple CVEs in Spring Boot <2.7"},
            {"Log4j", "2\\.(0|1)\\..*", "CVE-2021-44228 (Log4Shell)"},
            {"Netty", "4\\.0\\..*|4\\.1\\.[0-9]\\..*|4\\.1\\.[1-5][0-9]\\..*", "Multiple CVEs"},
            {"Tomcat", "[89]\\..*|10\\.0\\.[0-9]\\b", "Multiple CVEs"},
    };

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        for (String[] entry : VERSION_CLASSES) {
            try {
                Object clazz = heapHolder.findClass(entry[0]);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String val = heapHolder.getFieldStringValue(instance, entry[1]);
                    if (val != null && !val.isEmpty()) {
                        fields.put("library", entry[2]);
                        fields.put("class", entry[0]);
                        fields.put("version", val);
                        String vuln = checkVulnerability(entry[2], val);
                        if (vuln != null) {
                            fields.put("VULNERABILITY", vuln);
                        }
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }

        // Also scan for log4j specifically
        try {
            Object clazz = heapHolder.findClass("org.apache.logging.log4j.core.Logger");
            if (clazz != null) {
                HashMap<String, String> fields = new HashMap<>();
                fields.put("library", "Log4j2");
                fields.put("class", "org.apache.logging.log4j.core.Logger");
                fields.put("note", "Log4j2 found in heap - check version for Log4Shell");
                result.append(HashMapUtils.dumpString(fields, false)).append("\n");
            }
        } catch (Exception ignored) {}

        return result.toString();
    }

    private String checkVulnerability(String library, String version) {
        for (String[] pattern : VULN_PATTERNS) {
            if (pattern[0].equals(library)) {
                try {
                    if (Pattern.matches(pattern[1], version)) {
                        return pattern[2];
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
