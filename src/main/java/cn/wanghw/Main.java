package cn.wanghw;

import cn.wanghw.report.HtmlReportRenderer;
import cn.wanghw.report.ScanReport;
import cn.wanghw.rule.*;
import cn.wanghw.repl.ReplRunner;
import cn.wanghw.web.WebServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.graalvm.visualvm.lib.jfluid.heap.GraalvmHeapHolder;
import org.netbeans.lib.profiler.heap.NetbeansHeapHolder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "heapdump-analyzer", mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Dig secrets out of JVM memory — Java HeapDump sensitive information analysis tool")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "Heap dump file path", arity = "0..1")
    private File heapFile;

    @Option(names = {"-s", "--spider"}, description = "Run specific spiders (comma-separated names or 'all')", split = ",")
    private List<String> spiderNames;

    @Option(names = {"-l", "--list"}, description = "List all available spiders and exit")
    private boolean listSpiders;

    @Option(names = {"-f", "--format"}, description = "Output format: text, json, csv", defaultValue = "text")
    private String format;

    @Option(names = {"-o", "--output"}, description = "Output file path (default: stdout)")
    private String outputPath;

    @Option(names = {"-e", "--extract"}, description = "Extract all strings matching regex pattern")
    private String extractPattern;

    @Option(names = {"--severity"}, description = "Minimum severity level: CRITICAL, HIGH, MEDIUM, LOW, INFO", defaultValue = "INFO")
    private String minSeverity;

    @Option(names = {"--gui"}, description = "Launch GUI mode")
    private boolean guiMode;

    @Option(names = {"--rules"}, description = "Enable rule engine with custom rules directory (default: built-in rules)")
    private String rulesDir = "";

    @Option(names = {"--rules-only"}, description = "Run only rule engine, skip Spider plugins")
    private boolean rulesOnly;

    @Option(names = {"--validate"}, description = "Enable offline credential format validation (no network)")
    private boolean validateEnabled;

    @Option(names = {"--validate-live"}, description = "Enable ONLINE credential validation (calls cloud APIs — may trigger alerts!)")
    private boolean validateLiveEnabled;

    @Option(names = {"--list-rules"}, description = "List all available rules and exit")
    private boolean listRules;

    @Option(names = {"--repl"}, description = "Launch interactive REPL mode for heap exploration")
    private boolean replMode;

    @Option(names = {"--parallel"}, description = "Enable parallel scanning (uses multiple threads)")
    private boolean parallelMode;

    @Option(names = {"--threads"}, description = "Number of threads for parallel scan (default: CPU cores)", defaultValue = "0")
    private int threadCount;

    @Option(names = {"--desktop"}, description = "Launch JavaFX Desktop GUI mode")
    private boolean desktopMode;

    @Option(names = {"--web"}, description = "Launch the Web UI server (browser dashboard)")
    private boolean webMode;

    @Option(names = {"--port"}, description = "Web UI port (default: 9090)", defaultValue = "9090")
    private int webPort;

    @Option(names = {"--batch"}, description = "Batch scan every heap dump file in a directory")
    private File batchDir;

    private final List<ISpider> allSpiders = SpiderRegistry.getInstance().getSpiders();

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"--desktop"};
        }
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (listSpiders) {
            listSpiders();
            return 0;
        }

        if (listRules) {
            listRules();
            return 0;
        }

        if (guiMode || desktopMode) {
            String[] guiArgs = (heapFile != null && heapFile.exists())
                    ? new String[]{heapFile.getAbsolutePath()}
                    : new String[0];
            com.heapdump.analyzer.ui.HeapDumpGUI.launchGUI(guiArgs);
            return 0;
        }

        if (webMode) {
            WebServer server = new WebServer(webPort);
            server.start();
            return 0;
        }

        if (batchDir != null) {
            return runBatch();
        }

        if (heapFile == null && !replMode) {
            System.err.println("Error: Please specify a heap dump file path.");
            System.err.println("Usage: heapdump-analyzer <heapfile> [options]");
            return 1;
        }

        if (heapFile == null) {
            System.err.println("Error: REPL mode requires a heap dump file.");
            return 1;
        }

        if (!heapFile.exists() || !heapFile.isFile()) {
            System.err.println("Error: File not found: " + heapFile.getAbsolutePath());
            return 1;
        }

        IHeapHolder heapHolder = createHeapHolder(heapFile);

        if (replMode) {
            ReplRunner repl = new ReplRunner(heapHolder, allSpiders);
            repl.run();
            return 0;
        }

        PrintStream out = System.out;
        if (outputPath != null) {
            out = new PrintStream(new FileOutputStream(outputPath), true);
        }

        if (extractPattern != null) {
            return extractStrings(heapHolder, out);
        }

        Severity minSev = Severity.valueOf(minSeverity.toUpperCase());

        if (validateLiveEnabled) {
            System.err.println("[!] --validate-live enabled: real outbound API calls will be made.");
            System.err.println("    This may trigger cloud threat-detection alerts (e.g. AWS GuardDuty).");
        }

        ScanReport report = scan(heapFile, heapHolder, minSev);
        render(report, out);
        return 0;
    }

    /** Configure scan options programmatically (used by the Web UI, which bypasses picocli). */
    public void setWebScanOptions(boolean validate, boolean validateLive, boolean parallel, boolean rulesOnly, int threads) {
        this.validateEnabled = validate;
        this.validateLiveEnabled = validateLive;
        this.parallelMode = parallel;
        this.rulesOnly = rulesOnly;
        this.threadCount = threads;
    }

    /** Run a single scan and assemble a {@link ScanReport}. Shared by CLI, Web UI and batch. */
    public ScanReport scan(File file, IHeapHolder heapHolder, Severity minSev) {
        List<ISpider> spiders = rulesOnly ? java.util.Collections.emptyList() : resolveSpiders();
        java.util.Map<String, String> spiderData = executeSpiders(spiders, heapHolder, minSev);

        List<ScanReport.SpiderEntry> spiderEntries = new ArrayList<>();
        for (ISpider spider : spiders) {
            if (spider.getSeverity().ordinal() > minSev.ordinal()) continue;
            String data = spiderData.get(spider.getName());
            boolean found = data != null && !data.isEmpty();
            spiderEntries.add(new ScanReport.SpiderEntry(
                    spider.getName(), spider.getCategory(), spider.getSeverity(),
                    spider.getDescription(), found, found ? data : ""));
        }

        RuleEngine engine = createRuleEngine();
        List<ScanReport.RuleEntry> ruleEntries = new ArrayList<>();
        if (engine != null && engine.getRuleCount() > 0) {
            engine.setMinSeverity(minSev);
            engine.setValidateEnabled(validateEnabled);
            engine.setValidateLiveEnabled(validateLiveEnabled);
            engine.setParallelEnabled(parallelMode);
            engine.setThreadCount(threadCount);
            List<RuleResult> ruleResults = engine.execute(heapHolder);
            for (RuleResult rr : ruleResults) {
                ruleEntries.add(ScanReport.RuleEntry.from(rr));
            }
        }

        String scanTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return new ScanReport(
                file.getName(), file.length(), scanTime, "1.0.0",
                spiders.size(), engine != null ? engine.getRuleCount() : 0,
                spiderEntries, ruleEntries);
    }

    private void render(ScanReport report, PrintStream out) {
        switch (format.toLowerCase()) {
            case "json" -> renderJson(report, out);
            case "csv" -> renderCsv(report, out);
            case "html" -> out.println(HtmlReportRenderer.render(report));
            default -> renderText(report, out);
        }
    }

    private void renderText(ScanReport report, PrintStream out) {
        out.println("===========================================");
        out.println("HeapDump Analyzer v" + report.getVersion() + " — " + report.getFileName());
        out.println("Scanned: " + report.getScanTime());
        out.println("===========================================");
        Map<Severity, Integer> sev = report.severityCounts();
        out.println("Severity: " + sev.get(Severity.CRITICAL) + " Critical / "
                + sev.get(Severity.HIGH) + " High / " + sev.get(Severity.MEDIUM) + " Medium / "
                + sev.get(Severity.LOW) + " Low / " + sev.get(Severity.INFO) + " Info");
        if (report.liveCount() > 0) {
            out.println("[!] " + report.liveCount() + " LIVE credential(s) confirmed online");
        }
        out.println("===========================================");

        for (ScanReport.SpiderEntry s : report.getHitSpiders()) {
            out.println(s.name + " [" + s.severity.getLabel() + "]");
            out.println("-------------");
            out.println(s.data);
        }
        if (!report.getHitRules().isEmpty()) {
            out.println("===========================================");
            out.println("Rule Engine Results (" + report.getHitRules().size() + " rules matched)");
            out.println("===========================================");
            for (ScanReport.RuleEntry r : report.getHitRules()) {
                out.println("[" + r.name + "] " + r.description);
                for (String m : r.matches) out.println("  " + m);
                if (!r.validated.isEmpty()) {
                    out.println("  ── Offline Validation ──");
                    for (String v : r.validated) out.println("  " + v);
                }
                if (!r.liveResults.isEmpty()) {
                    out.println("  ── Live Validation ──");
                    for (var c : r.liveResults) out.println("  " + c);
                }
            }
        }
        out.println("===========================================");
    }

    private void renderJson(ScanReport report, PrintStream out) {
        JsonObject root = new JsonObject();
        root.addProperty("file", report.getFileName());
        root.addProperty("scanTime", report.getScanTime());
        root.addProperty("version", report.getVersion());

        JsonObject sev = new JsonObject();
        for (var e : report.severityCounts().entrySet()) sev.addProperty(e.getKey().name(), e.getValue());
        root.add("severity", sev);
        root.addProperty("liveCredentials", report.liveCount());

        JsonArray spiders = new JsonArray();
        for (ScanReport.SpiderEntry s : report.getHitSpiders()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", s.name);
            o.addProperty("category", s.category);
            o.addProperty("severity", s.severity.name());
            o.addProperty("description", s.description);
            o.addProperty("data", s.data);
            spiders.add(o);
        }
        root.add("spiders", spiders);

        JsonArray rules = new JsonArray();
        for (ScanReport.RuleEntry r : report.getHitRules()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.id);
            o.addProperty("name", r.name);
            o.addProperty("category", r.category);
            o.addProperty("severity", r.severity.name());
            o.addProperty("description", r.description);
            o.add("matches", new Gson().toJsonTree(r.matches));
            if (!r.validated.isEmpty()) o.add("validated", new Gson().toJsonTree(r.validated));
            if (!r.liveResults.isEmpty()) {
                JsonArray live = new JsonArray();
                for (var c : r.liveResults) {
                    JsonObject lc = new JsonObject();
                    lc.addProperty("candidate", c.getCandidate());
                    lc.addProperty("status", c.getStatus().name());
                    lc.addProperty("detail", c.getDetail());
                    live.add(lc);
                }
                o.add("liveResults", live);
            }
            rules.add(o);
        }
        root.add("rules", rules);

        out.println(new GsonBuilder().setPrettyPrinting().create().toJson(root));
    }

    private void renderCsv(ScanReport report, PrintStream out) {
        out.println("type,name,category,severity,data");
        for (ScanReport.SpiderEntry s : report.getHitSpiders()) {
            out.printf("\"spider\",\"%s\",\"%s\",\"%s\",%s%n",
                    s.name, s.category, s.severity.name(), csvEscape(s.data));
        }
        for (ScanReport.RuleEntry r : report.getHitRules()) {
            StringBuilder data = new StringBuilder();
            for (String m : r.matches) data.append(m).append(" | ");
            out.printf("\"rule\",\"%s\",\"%s\",\"%s\",%s%n",
                    r.name, r.category, r.severity.name(), csvEscape(data.toString()));
        }
    }

    private String csvEscape(String s) {
        if (s == null || s.isEmpty()) return "\"\"";
        return "\"" + s.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private int runBatch() {
        if (!batchDir.exists() || !batchDir.isDirectory()) {
            System.err.println("Error: Directory not found: " + batchDir.getAbsolutePath());
            return 1;
        }
        File[] files = batchDir.listFiles((d, n) ->
                n.endsWith(".hprof") || n.endsWith(".bin") || n.endsWith(".heapdump"));
        if (files == null || files.length == 0) {
            System.err.println("No heap dump files (.hprof/.bin/.heapdump) found in: " + batchDir.getAbsolutePath());
            return 1;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        System.err.println("[+] Batch scanning " + files.length + " file(s) in " + batchDir.getAbsolutePath());

        Severity minSev = Severity.valueOf(minSeverity.toUpperCase());
        String outDir = outputPath != null ? outputPath : batchDir.getAbsolutePath();
        int ok = 0;
        for (File f : files) {
            System.err.println("\n===========================================");
            System.err.println("Scanning: " + f.getName());
            try {
                IHeapHolder holder = createHeapHolder(f);
                ScanReport report = scan(f, holder, minSev);
                String reportPath = outDir + File.separator + f.getName() + ".html";
                try (PrintStream pout = new PrintStream(new FileOutputStream(reportPath))) {
                    pout.println(HtmlReportRenderer.render(report));
                }
                System.err.println("  -> " + report.getHitSpiders().size() + " spider hits, "
                        + report.getHitRules().size() + " rule hits, "
                        + report.liveCount() + " LIVE creds -> " + reportPath);
                ok++;
            } catch (Exception e) {
                System.err.println("  FAILED: " + e.getMessage());
            }
        }
        System.err.println("\n[+] Batch complete: " + ok + "/" + files.length + " succeeded.");
        return 0;
    }

    private RuleEngine createRuleEngine() {
        List<Rule> rules = YamlRuleLoader.loadAll(rulesDir.isEmpty() ? null : rulesDir);
        return new RuleEngine(rules);
    }

    private void listRules() {
        List<Rule> rules = YamlRuleLoader.loadAll(rulesDir.isEmpty() ? null : rulesDir);
        System.out.println("Available Rules (" + rules.size() + "):");
        System.out.println("=".repeat(80));
        System.out.printf("%-30s %-12s %-10s %s%n", "ID", "Category", "Severity", "Name");
        System.out.println("-".repeat(80));
        for (Rule rule : rules) {
            System.out.printf("%-30s %-12s %-10s %s%n",
                    rule.getId(),
                    rule.getCategory(),
                    rule.getSeverity().getLabel(),
                    rule.getName());
        }
    }

    public IHeapHolder createHeapHolder(File file) throws Exception {
        int ver = getFileVersion(file);
        float classVersion = Float.parseFloat(System.getProperty("java.class.version"));
        if (ver == 1 || classVersion < 52) {
            return new NetbeansHeapHolder(file);
        } else {
            return new GraalvmHeapHolder(file);
        }
    }

    private void listSpiders() {
        System.out.println("Available Spiders:");
        System.out.println("==================");
        System.out.printf("%-30s %-12s %-10s %s%n", "Name", "Category", "Severity", "Description");
        System.out.println("-".repeat(80));
        for (ISpider spider : allSpiders) {
            System.out.printf("%-30s %-12s %-10s %s%n",
                    spider.getName(),
                    spider.getCategory(),
                    spider.getSeverity().getLabel(),
                    spider.getDescription());
        }
    }

    private List<ISpider> resolveSpiders() {
        if (spiderNames == null || spiderNames.isEmpty() || spiderNames.contains("all")) {
            return allSpiders;
        }
        List<ISpider> result = new ArrayList<>();
        Map<String, ISpider> nameMap = new HashMap<>();
        for (ISpider spider : allSpiders) {
            nameMap.put(spider.getName().toLowerCase(), spider);
        }
        for (String name : spiderNames) {
            ISpider spider = nameMap.get(name.toLowerCase());
            if (spider != null) {
                result.add(spider);
            } else {
                System.err.println("Warning: Spider not found: " + name);
            }
        }
        return result;
    }

    private int extractStrings(IHeapHolder heapHolder, PrintStream out) {
        try {
            List<String> matches = heapHolder.searchStrings(java.util.regex.Pattern.compile(extractPattern));
            for (String s : matches) {
                out.println(s);
            }
            out.println("\n[+] Found " + matches.size() + " matching strings.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    private java.util.Map<String, String> executeSpiders(List<ISpider> spiders, IHeapHolder heapHolder, Severity minSev) {
        List<ISpider> filtered = new ArrayList<>();
        for (ISpider spider : spiders) {
            if (spider.getSeverity().ordinal() <= minSev.ordinal()) {
                filtered.add(spider);
            }
        }
        if (parallelMode) {
            int threads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors();
            System.err.println("[+] Parallel scanning with " + threads + " threads...");
            ParallelScanner scanner = new ParallelScanner(heapHolder, threads);
            return scanner.scan(filtered, minSev);
        } else {
            java.util.LinkedHashMap<String, String> results = new java.util.LinkedHashMap<>();
            for (ISpider spider : filtered) {
                try {
                    String result = spider.sniff(heapHolder);
                    results.put(spider.getName(), result != null ? result : "");
                } catch (Exception e) {
                    results.put(spider.getName(), "ERROR: " + e.getMessage());
                }
            }
            return results;
        }
    }

    public int getFileVersion(File file) {
        try (FileInputStream io = new FileInputStream(file)) {
            io.skip(17);
            byte subVersion = (byte) io.read();
            return Integer.parseInt(Character.valueOf((char) subVersion).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Backward compatibility: programmatic API
    public static String run(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bout);
        Main _main = new Main();
        if (args.length < 1) {
            out.println("please give a heap filepath.");
        } else {
            _main.heapFile = new File(args[0]);
            if (_main.heapFile.exists() && _main.heapFile.isFile()) {
                IHeapHolder heapHolder = _main.createHeapHolder(_main.heapFile);
                Severity minSev = Severity.INFO;
                ScanReport report = _main.scan(_main.heapFile, heapHolder, minSev);
                _main.renderText(report, out);
            } else {
                out.println("file not exist!");
            }
        }
        return bout.toString();
    }
}
