package cn.wanghw.rule;

import cn.wanghw.Severity;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class YamlRuleLoader {

    private static final Yaml YAML = new Yaml();

    /**
     * Load rules from classpath resources (rules/ directory inside JAR)
     */
    public static List<Rule> loadFromClasspath() {
        List<Rule> rules = new ArrayList<>();
        try {
            // Load built-in rules from resources
            InputStream index = YamlRuleLoader.class.getClassLoader()
                    .getResourceAsStream("rules/rule-index.txt");
            if (index != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(index));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    InputStream ruleStream = YamlRuleLoader.class.getClassLoader()
                            .getResourceAsStream("rules/" + line);
                    if (ruleStream != null) {
                        rules.addAll(parseRules(ruleStream));
                    }
                }
            } else {
                // No index file, try loading all .yml files from rules/ subdirectories
                loadFromDirectoryInJar(rules);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load built-in rules: " + e.getMessage());
        }
        return rules;
    }

    /**
     * Load rules from an external directory on the filesystem
     */
    public static List<Rule> loadFromDirectory(String dirPath) {
        List<Rule> rules = new ArrayList<>();
        Path dir = Paths.get(dirPath);
        if (!Files.isDirectory(dir)) {
            System.err.println("Warning: Rules directory not found: " + dirPath);
            return rules;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                 .forEach(p -> {
                     try (InputStream is = Files.newInputStream(p)) {
                         rules.addAll(parseRules(is));
                     } catch (Exception e) {
                         System.err.println("Warning: Failed to load rule file " + p + ": " + e.getMessage());
                     }
                 });
        } catch (Exception e) {
            System.err.println("Warning: Failed to scan rules directory: " + e.getMessage());
        }
        return rules;
    }

    /**
     * Load all rules: built-in (classpath) + user home ({@code ~/.heapdump-analyzer/rules/})
     * + an explicitly provided directory.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>built-in {@code resources/rules/}</li>
     *   <li>{@code ~/.heapdump-analyzer/rules/} — drop custom rules here, auto-loaded</li>
     *   <li>{@code customRulesDir} — when {@code --rules <dir>} is passed</li>
     * </ol>
     */
    public static List<Rule> loadAll(String customRulesDir) {
        List<Rule> rules = new ArrayList<>();
        rules.addAll(loadFromClasspath());

        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            Path userRules = Paths.get(home, ".heapdump-analyzer", "rules");
            if (Files.isDirectory(userRules)) {
                rules.addAll(loadFromDirectory(userRules.toString()));
            }
        }

        if (customRulesDir != null && !customRulesDir.isEmpty()) {
            rules.addAll(loadFromDirectory(customRulesDir));
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private static List<Rule> parseRules(InputStream inputStream) {
        List<Rule> rules = new ArrayList<>();
        Iterable<Object> docs = YAML.loadAll(inputStream);
        for (Object doc : docs) {
            if (!(doc instanceof Map)) continue;
            Map<String, Object> root = (Map<String, Object>) doc;
            String kind = (String) root.get("kind");
            if (kind == null) continue;

            Map<String, Object> metadata = (Map<String, Object>) root.get("metadata");
            Map<String, Object> spec = (Map<String, Object>) root.get("spec");
            if (metadata == null || spec == null) continue;

            String id = (String) metadata.getOrDefault("id", "unknown");
            String name = (String) metadata.getOrDefault("name", "Unnamed Rule");
            String category = (String) metadata.getOrDefault("category", "default");
            String description = (String) metadata.getOrDefault("description", "");
            String severityStr = (String) metadata.getOrDefault("severity", "MEDIUM");
            Severity severity;
            try {
                severity = Severity.valueOf(severityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                severity = Severity.MEDIUM;
            }

            switch (kind) {
                case "RegexRule":
                    rules.add(buildRegexRule(id, name, category, severity, description, spec));
                    break;
                case "ClassRule":
                    rules.add(buildClassRule(id, name, category, severity, description, spec));
                    break;
            }
        }
        return rules;
    }

    private static RegexRule buildRegexRule(String id, String name, String category,
                                            Severity severity, String description,
                                            Map<String, Object> spec) {
        String patternStr = (String) spec.get("pattern");
        if (patternStr == null) return null;
        Pattern pattern = Pattern.compile(patternStr);
        RegexRule rule = new RegexRule(id, name, category, severity, description, pattern);
        rule.setValidator(resolveValidator((String) spec.get("validator")));
        return rule;
    }

    @SuppressWarnings("unchecked")
    private static ClassRule buildClassRule(String id, String name, String category,
                                            Severity severity, String description,
                                            Map<String, Object> spec) {
        ClassRule rule = new ClassRule();
        rule.setId(id);
        rule.setName(name);
        rule.setCategory(category);
        rule.setSeverity(severity);
        rule.setDescription(description);
        rule.setClassName((String) spec.get("className"));

        List<String> fields = (List<String>) spec.get("fields");
        if (fields != null) {
            rule.setFieldNames(fields);
        }
        List<String> alternates = (List<String>) spec.get("alternateClassNames");
        if (alternates != null) {
            rule.setAlternateClassNames(alternates);
        }
        rule.setValidator(resolveValidator((String) spec.get("validator")));
        return rule;
    }

    private static Validator resolveValidator(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            String cls = name.contains(".") ? name : "cn.wanghw.rule.validator." + name;
            return (Validator) Class.forName(cls).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Warning: Failed to load validator '" + name + "': " + e.getMessage());
            return null;
        }
    }

    private static void loadFromDirectoryInJar(List<Rule> rules) {
        // Fallback: load well-known rule files
        String[] categories = {"cloud", "database", "auth", "mq", "framework", "general"};
        for (String cat : categories) {
            String[] ruleFiles = getRuleFilesForCategory(cat);
            for (String ruleFile : ruleFiles) {
                try (InputStream is = YamlRuleLoader.class.getClassLoader()
                        .getResourceAsStream("rules/" + cat + "/" + ruleFile)) {
                    if (is != null) {
                        rules.addAll(parseRules(is));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private static String[] getRuleFilesForCategory(String category) {
        return switch (category) {
            case "cloud" -> new String[]{"aws-access-key.yml", "aliyun-ak-sk.yml", "gcp-service-account.yml",
                    "azure-connection-string.yml", "huawei-ak-sk.yml", "tencent-ak-sk.yml",
                    "docker-registry.yml", "k8s-service-account.yml"};
            case "database" -> new String[]{"spring-datasource.yml", "hikaricp-detail.yml",
                    "clickhouse.yml", "hbase.yml", "neo4j.yml", "influxdb.yml"};
            case "auth" -> new String[]{"jwt-token.yml", "oauth2-token.yml", "bearer-token.yml",
                    "github-token.yml", "gitlab-token.yml", "slack-token.yml",
                    "stripe-key.yml", "private-key.yml"};
            case "mq" -> new String[]{"rocketmq.yml", "pulsar.yml", "activemq.yml"};
            case "framework" -> new String[]{"spring-security.yml", "sa-token.yml", "shiro.yml"};
            case "general" -> new String[]{"generic-api-key.yml", "jdbc-connection.yml", "smtp-credential.yml"};
            default -> new String[]{};
        };
    }
}
