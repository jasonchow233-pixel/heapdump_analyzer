package cn.wanghw.report;

import cn.wanghw.Severity;
import cn.wanghw.rule.CredentialCheckResult;
import cn.wanghw.rule.CredentialStatus;
import cn.wanghw.rule.RuleResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable, serializable aggregate of a single heap-dump scan. Acts as the common data
 * model shared by the CLI text/json/csv/html renderers, the Web UI JSON API and the
 * JavaFX table — so all four surfaces show identical information.
 */
public final class ScanReport {

    private final String fileName;
    private final long fileSize;
    private final String scanTime;
    private final String version;
    private final int spiderCount;
    private final int ruleCount;
    private final List<SpiderEntry> spiders;
    private final List<RuleEntry> rules;

    public ScanReport(String fileName, long fileSize, String scanTime, String version,
                      int spiderCount, int ruleCount,
                      List<SpiderEntry> spiders, List<RuleEntry> rules) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.scanTime = scanTime;
        this.version = version;
        this.spiderCount = spiderCount;
        this.ruleCount = ruleCount;
        this.spiders = spiders;
        this.rules = rules;
    }

    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getScanTime() { return scanTime; }
    public String getVersion() { return version; }
    public int getSpiderCount() { return spiderCount; }
    public int getRuleCount() { return ruleCount; }
    public List<SpiderEntry> getSpiders() { return spiders; }
    public List<RuleEntry> getRules() { return rules; }

    /** Only spiders that actually found something. */
    public List<SpiderEntry> getHitSpiders() {
        List<SpiderEntry> hits = new ArrayList<>();
        for (SpiderEntry s : spiders) if (s.found) hits.add(s);
        return hits;
    }

    public List<RuleEntry> getHitRules() {
        List<RuleEntry> hits = new ArrayList<>();
        for (RuleEntry r : rules) if (r.found) hits.add(r);
        return hits;
    }

    public Map<Severity, Integer> severityCounts() {
        Map<Severity, Integer> counts = new LinkedHashMap<>();
        for (Severity s : Severity.values()) counts.put(s, 0);
        for (SpiderEntry s : spiders) if (s.found) counts.merge(s.severity, 1, Integer::sum);
        for (RuleEntry r : rules) if (r.found) counts.merge(r.severity, 1, Integer::sum);
        return counts;
    }

    public Map<String, Integer> categoryCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SpiderEntry s : spiders) if (s.found) counts.merge(s.category, 1, Integer::sum);
        for (RuleEntry r : rules) if (r.found) counts.merge(r.category, 1, Integer::sum);
        return counts;
    }

    public int liveCount() {
        int n = 0;
        for (RuleEntry r : rules) {
            for (CredentialCheckResult c : r.liveResults) if (c.getStatus() == CredentialStatus.LIVE) n++;
        }
        return n;
    }

    public static final class SpiderEntry {
        public final String name;
        public final String category;
        public final Severity severity;
        public final String description;
        public final boolean found;
        public final String data;

        public SpiderEntry(String name, String category, Severity severity, String description, boolean found, String data) {
            this.name = name;
            this.category = category;
            this.severity = severity;
            this.description = description;
            this.found = found;
            this.data = data != null ? data : "";
        }
    }

    public static final class RuleEntry {
        public final String id;
        public final String name;
        public final String category;
        public final Severity severity;
        public final String description;
        public final boolean found;
        public final List<String> matches;
        public final List<String> validated;
        public final List<CredentialCheckResult> liveResults;

        public RuleEntry(String id, String name, String category, Severity severity, String description,
                         boolean found, List<String> matches, List<String> validated, List<CredentialCheckResult> liveResults) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.severity = severity;
            this.description = description;
            this.found = found;
            this.matches = matches != null ? matches : new ArrayList<>();
            this.validated = validated != null ? validated : new ArrayList<>();
            this.liveResults = liveResults != null ? liveResults : new ArrayList<>();
        }

        public static RuleEntry from(RuleResult rr) {
            return new RuleEntry(
                    rr.getRule().getId(),
                    rr.getRule().getName(),
                    rr.getRule().getCategory(),
                    rr.getRule().getSeverity(),
                    rr.getRule().getDescription(),
                    rr.isFound(),
                    rr.getMatches(),
                    rr.getValidated(),
                    rr.getLiveResults()
            );
        }
    }
}
