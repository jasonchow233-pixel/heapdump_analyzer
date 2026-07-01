package cn.wanghw.repl;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.rule.Rule;
import cn.wanghw.rule.RuleEngine;
import cn.wanghw.rule.RuleResult;
import cn.wanghw.rule.YamlRuleLoader;
import cn.wanghw.Severity;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Interactive REPL (Read-Eval-Print Loop) for heap dump exploration.
 * Inspired by heapdump_tool's interactive query mode.
 *
 * Commands:
 *   help                    - Show available commands
 *   classes <pattern>       - Search classes by name pattern (supports * wildcard)
 *   instances <class>       - List instances of a class
 *   fields <class>          - Show fields of a class
 *   getfield <class> <field> - Get field value from first instance
 *   strings <regex>         - Search all strings matching regex
 *   search <keyword>        - Search strings containing keyword
 *   spider [name|all]       - Run spider(s)
 *   rules                   - Run rule engine
 *   stats                   - Show heap statistics
 *   export <spider|rules|all> <filepath> - Export results to file
 *   severity <level>        - Set minimum severity filter
 *   quit                    - Exit REPL
 */
public class ReplRunner {

    private final IHeapHolder heapHolder;
    private final List<ISpider> allSpiders;
    private final Scanner scanner;
    private Severity minSeverity = Severity.INFO;
    private boolean validateEnabled = false;
    private Map<String, String> cachedSpiderResults;
    private List<RuleResult> cachedRuleResults;

    public ReplRunner(IHeapHolder heapHolder, List<ISpider> spiders) {
        this.heapHolder = heapHolder;
        this.allSpiders = spiders;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   HeapDump Analyzer Interactive REPL v1.0        ║");
        System.out.println("║   Type 'help' for available commands             ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        while (true) {
            System.out.print("heapdump> ");
            System.out.flush();
            String line;
            try {
                line = scanner.nextLine().trim();
            } catch (NoSuchElementException e) {
                break;
            }
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                System.out.println("Bye!");
                break;
            }
            try {
                executeCommand(line);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void executeCommand(String line) throws Exception {
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help":
                showHelp();
                break;
            case "classes":
                searchClasses(parts.length > 1 ? parts[1] : "*");
                break;
            case "instances":
                if (parts.length < 2) {
                    System.err.println("Usage: instances <classname>");
                    return;
                }
                listInstances(parts[1]);
                break;
            case "fields":
                if (parts.length < 2) {
                    System.err.println("Usage: fields <classname>");
                    return;
                }
                showFields(parts[1]);
                break;
            case "getfield":
                if (parts.length < 3) {
                    System.err.println("Usage: getfield <classname> <fieldname>");
                    return;
                }
                getField(parts[1], parts[2]);
                break;
            case "strings":
                if (parts.length < 2) {
                    System.err.println("Usage: strings <regex>");
                    return;
                }
                searchStrings(parts[1]);
                break;
            case "search":
                if (parts.length < 2) {
                    System.err.println("Usage: search <keyword>");
                    return;
                }
                searchKeyword(parts[1]);
                break;
            case "spider":
                runSpiders(parts.length > 1 ? parts[1] : "all");
                break;
            case "rules":
                runRules();
                break;
            case "stats":
                showStats();
                break;
            case "export":
                if (parts.length < 3) {
                    System.err.println("Usage: export <spider|rules|all> <filepath>");
                    return;
                }
                exportResults(parts[1], parts[2]);
                break;
            case "severity":
                if (parts.length < 2) {
                    System.out.println("Current severity: " + minSeverity);
                    return;
                }
                minSeverity = Severity.valueOf(parts[1].toUpperCase());
                System.out.println("Severity set to: " + minSeverity);
                break;
            case "validate":
                validateEnabled = !validateEnabled;
                System.out.println("Validation " + (validateEnabled ? "enabled" : "disabled"));
                break;
            default:
                System.err.println("Unknown command: " + cmd + ". Type 'help' for available commands.");
        }
    }

    private void showHelp() {
        System.out.println("Available Commands:");
        System.out.println("─────────────────────────────────────────────────────");
        System.out.println("  help                          Show this help");
        System.out.println("  classes <pattern>             Search classes (* wildcard supported)");
        System.out.println("  instances <class>             List instances of a class");
        System.out.println("  fields <class>                Show declared fields of a class");
        System.out.println("  getfield <class> <field>      Get field value from first instance");
        System.out.println("  strings <regex>               Search all strings matching regex");
        System.out.println("  search <keyword>              Search strings containing keyword");
        System.out.println("  spider [name|all]             Run spider(s) and show results");
        System.out.println("  rules                         Run rule engine and show results");
        System.out.println("  stats                         Show heap file statistics");
        System.out.println("  severity [level]              Get/set minimum severity (CRITICAL/HIGH/MEDIUM/LOW/INFO)");
        System.out.println("  validate                      Toggle credential validation on/off");
        System.out.println("  export <spider|rules|all> <f> Export results to file");
        System.out.println("  quit                          Exit REPL");
        System.out.println();
    }

    private void searchClasses(String pattern) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        java.util.Iterator it = heapHolder.getClasses();
        int count = 0;
        while (it.hasNext()) {
            Object clazz = it.next();
            String name = heapHolder.getClassName(clazz);
            if (name.matches(regex) || name.toLowerCase().contains(pattern.toLowerCase().replace("*", ""))) {
                System.out.println("  " + name);
                count++;
                if (count >= 100) {
                    System.out.println("  ... (truncated, 100+ results)");
                    break;
                }
            }
        }
        System.out.println("[+] Found " + count + " matching classes.");
    }

    private void listInstances(String className) {
        try {
            Object clazz = heapHolder.findClass(className);
            if (clazz == null) {
                System.err.println("Class not found: " + className);
                return;
            }
            List instances = heapHolder.getInstances(clazz);
            System.out.println("[+] " + instances.size() + " instances of " + className);
            int limit = Math.min(instances.size(), 20);
            for (int i = 0; i < limit; i++) {
                java.util.HashMap<String, String> fields = heapHolder.getAllFieldValues(instances.get(i));
                System.out.println("  Instance #" + (i + 1) + ": " + fields);
            }
            if (instances.size() > limit) {
                System.out.println("  ... (" + (instances.size() - limit) + " more instances)");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void showFields(String className) {
        try {
            Object clazz = heapHolder.findClass(className);
            if (clazz == null) {
                System.err.println("Class not found: " + className);
                return;
            }
            // Get fields from first instance or class definition
            List instances = heapHolder.getInstances(clazz);
            if (!instances.isEmpty()) {
                java.util.HashMap<String, String> fields = heapHolder.getAllFieldValues(instances.get(0));
                System.out.println("Fields of " + className + ":");
                for (Map.Entry<String, String> e : fields.entrySet()) {
                    System.out.println("  " + e.getKey() + " = " + e.getValue());
                }
            } else {
                System.out.println("No instances found for " + className);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void getField(String className, String fieldName) {
        try {
            Object clazz = heapHolder.findClass(className);
            if (clazz == null) {
                System.err.println("Class not found: " + className);
                return;
            }
            List instances = heapHolder.getInstances(clazz);
            int limit = Math.min(instances.size(), 10);
            for (int i = 0; i < limit; i++) {
                String val = heapHolder.getFieldStringValue(instances.get(i), fieldName);
                System.out.println("  Instance #" + (i + 1) + "." + fieldName + " = " + val);
            }
            if (instances.size() > limit) {
                System.out.println("  ... (" + (instances.size() - limit) + " more)");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void searchStrings(String regex) {
        try {
            List<String> matches = heapHolder.searchStrings(Pattern.compile(regex));
            int limit = Math.min(matches.size(), 50);
            for (int i = 0; i < limit; i++) {
                System.out.println("  " + matches.get(i));
            }
            if (matches.size() > limit) {
                System.out.println("  ... (" + (matches.size() - limit) + " more)");
            }
            System.out.println("[+] Found " + matches.size() + " matching strings.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void searchKeyword(String keyword) {
        searchStrings(Pattern.quote(keyword));
    }

    private void runSpiders(String nameOrAll) {
        List<ISpider> toRun = new ArrayList<>();
        if (nameOrAll.equalsIgnoreCase("all")) {
            for (ISpider s : allSpiders) {
                if (s.getSeverity().ordinal() <= minSeverity.ordinal()) {
                    toRun.add(s);
                }
            }
        } else {
            for (ISpider s : allSpiders) {
                if (s.getName().equalsIgnoreCase(nameOrAll)) {
                    toRun.add(s);
                    break;
                }
            }
            if (toRun.isEmpty()) {
                System.err.println("Spider not found: " + nameOrAll);
                return;
            }
        }

        java.util.LinkedHashMap<String, String> results = new java.util.LinkedHashMap<>();
        for (ISpider spider : toRun) {
            try {
                String result = spider.sniff(heapHolder);
                results.put(spider.getName(), result != null ? result : "");
            } catch (Exception e) {
                results.put(spider.getName(), "ERROR: " + e.getMessage());
            }
        }

        cachedSpiderResults = results;
        int found = 0;
        for (Map.Entry<String, String> e : results.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                System.out.println("━━━ " + e.getKey() + " ━━━");
                System.out.println(e.getValue());
                found++;
            }
        }
        System.out.println("[+] " + found + "/" + toRun.size() + " spiders found results.");
    }

    private void runRules() {
        List<Rule> rules = YamlRuleLoader.loadAll(null);
        RuleEngine engine = new RuleEngine(rules);
        engine.setMinSeverity(minSeverity);
        engine.setValidateEnabled(validateEnabled);
        List<RuleResult> results = engine.execute(heapHolder);
        cachedRuleResults = results;
        for (RuleResult rr : results) {
            System.out.println(rr);
        }
        System.out.println("[+] " + results.size() + " rules matched.");
    }

    private void showStats() {
        try {
            java.util.Iterator it = heapHolder.getClasses();
            int classCount = 0;
            while (it.hasNext()) { it.next(); classCount++; }
            System.out.println("Heap Statistics:");
            System.out.println("  Total classes: " + classCount);
            System.out.println("  Minimum severity: " + minSeverity);
            System.out.println("  Validation: " + (validateEnabled ? "ON" : "OFF"));
            System.out.println("  Available spiders: " + allSpiders.size());
            List<Rule> rules = YamlRuleLoader.loadAll(null);
            System.out.println("  Available rules: " + rules.size());
            if (cachedSpiderResults != null) {
                long foundCount = cachedSpiderResults.values().stream()
                        .filter(v -> v != null && !v.isEmpty()).count();
                System.out.println("  Cached spider results: " + foundCount + "/" + cachedSpiderResults.size() + " found");
            }
            if (cachedRuleResults != null) {
                System.out.println("  Cached rule results: " + cachedRuleResults.size() + " matched");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void exportResults(String type, String filepath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            if (type.equalsIgnoreCase("spider") || type.equalsIgnoreCase("all")) {
                if (cachedSpiderResults == null) {
                    writer.println("No spider results cached. Run 'spider all' first.");
                } else {
                    for (Map.Entry<String, String> e : cachedSpiderResults.entrySet()) {
                        writer.println("=== " + e.getKey() + " ===");
                        writer.println(e.getValue());
                    }
                }
            }
            if (type.equalsIgnoreCase("rules") || type.equalsIgnoreCase("all")) {
                if (cachedRuleResults == null) {
                    writer.println("No rule results cached. Run 'rules' first.");
                } else {
                    for (RuleResult rr : cachedRuleResults) {
                        writer.println(rr);
                    }
                }
            }
            System.out.println("[+] Results exported to: " + filepath);
        } catch (Exception e) {
            System.err.println("Export error: " + e.getMessage());
        }
    }
}
