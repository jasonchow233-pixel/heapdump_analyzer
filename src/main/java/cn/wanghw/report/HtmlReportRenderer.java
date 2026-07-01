package cn.wanghw.report;

import cn.wanghw.Severity;
import cn.wanghw.rule.CredentialCheckResult;

import java.util.List;
import java.util.Map;

/**
 * Renders a {@link ScanReport} as a single self-contained HTML document.
 *
 * <p>No external CDN, no missing images — the output is a single {@code .html} file that
 * renders offline and can be emailed, Slack-shared or attached to a ticket. Charts are
 * inline SVG; filtering is inline vanilla JS.</p>
 */
public final class HtmlReportRenderer {

    private HtmlReportRenderer() {}

    public static String render(ScanReport report) {
        StringBuilder sb = new StringBuilder(64 * 1024);
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>HeapDump Analyzer Report — ").append(esc(report.getFileName())).append("</title>\n");
        sb.append("<style>\n");
        sb.append(CSS);
        sb.append("\n</style>\n</head>\n<body>\n");

        // Header
        sb.append("<header>\n");
        sb.append("  <div class=\"brand\">⛏ HeapDump Analyzer <span>v").append(esc(report.getVersion())).append("</span></div>\n");
        sb.append("  <div class=\"subtitle\">Dig secrets out of JVM memory</div>\n");
        sb.append("  <div class=\"meta\">\n");
        sb.append("    <div><b>File:</b> ").append(esc(report.getFileName())).append(" <span class=\"muted\">(").append(humanSize(report.getFileSize())).append(")</span></div>\n");
        sb.append("    <div><b>Scanned:</b> ").append(esc(report.getScanTime())).append("</div>\n");
        sb.append("    <div><b>Spiders:</b> ").append(report.getSpiderCount()).append(" · <b>Rules:</b> ").append(report.getRuleCount()).append("</div>\n");
        sb.append("  </div>\n");
        sb.append("</header>\n");

        // Severity cards
        Map<Severity, Integer> sev = report.severityCounts();
        int total = sev.values().stream().mapToInt(Integer::intValue).sum();
        sb.append("<section class=\"cards\">\n");
        for (Severity s : Severity.values()) {
            int c = sev.getOrDefault(s, 0);
            sb.append("  <div class=\"card\" style=\"border-top-color:").append(s.getColor()).append("\">\n");
            sb.append("    <div class=\"card-num\" style=\"color:").append(s.getColor()).append("\">").append(c).append("</div>\n");
            sb.append("    <div class=\"card-label\">").append(s.getLabel()).append("</div>\n");
            sb.append("  </div>\n");
        }
        sb.append("</section>\n");
        sb.append("<div class=\"total\">Total findings: <b>").append(total).append("</b>");
        int live = report.liveCount();
        if (live > 0) {
            sb.append(" · <span class=\"live-badge\">").append(live).append(" LIVE credential").append(live == 1 ? "" : "s").append("</span>");
        }
        sb.append("</div>\n");

        // Category chart
        Map<String, Integer> cats = report.categoryCounts();
        if (!cats.isEmpty()) {
            sb.append("<section>\n<h2>Findings by category</h2>\n");
            sb.append(renderCategoryChart(cats));
            sb.append("</section>\n");
        }

        // Live credential summary
        if (live > 0) {
            sb.append("<section>\n<h2>Live credential validation</h2>\n<table class=\"grid\">\n");
            sb.append("<thead><tr><th>Rule</th><th>Credential</th><th>Status</th><th>Detail</th></tr></thead><tbody>\n");
            for (ScanReport.RuleEntry r : report.getHitRules()) {
                for (CredentialCheckResult c : r.liveResults) {
                    sb.append("<tr><td>").append(esc(r.name)).append("</td>");
                    sb.append("<td><code>").append(esc(c.getCandidate())).append("</code></td>");
                    sb.append("<td><span class=\"status status-").append(c.getStatus().name().toLowerCase()).append("\">").append(c.getStatus()).append("</span></td>");
                    sb.append("<td>").append(esc(c.getDetail())).append("</td></tr>\n");
                }
            }
            sb.append("</tbody></table>\n</section>\n");
        }

        // Spider results
        List<ScanReport.SpiderEntry> spiderHits = report.getHitSpiders();
        sb.append("<section>\n<h2>Spider results <span class=\"muted\">(").append(spiderHits.size()).append(" hits)</span></h2>\n");
        sb.append("<input id=\"spider-filter\" class=\"filter\" placeholder=\"Filter spiders…\" oninput=\"filterTable('spider-filter','spider-table')\">\n");
        sb.append("<table id=\"spider-table\" class=\"grid\">\n<thead><tr><th>Name</th><th>Category</th><th>Severity</th><th>Data</th></tr></thead><tbody>\n");
        for (ScanReport.SpiderEntry s : spiderHits) {
            sb.append("<tr><td><b>").append(esc(s.name)).append("</b><div class=\"muted small\">").append(esc(s.description)).append("</div></td>");
            sb.append("<td>").append(esc(s.category)).append("</td>");
            sb.append("<td><span class=\"sev sev-").append(s.severity.name().toLowerCase()).append("\" style=\"--c:").append(s.severity.getColor()).append("\">").append(s.severity.getLabel()).append("</span></td>");
            sb.append("<td><pre>").append(esc(s.data)).append("</pre></td></tr>\n");
        }
        if (spiderHits.isEmpty()) {
            sb.append("<tr><td colspan=\"4\" class=\"empty\">No spider hits.</td></tr>\n");
        }
        sb.append("</tbody></table>\n</section>\n");

        // Rule results
        List<ScanReport.RuleEntry> ruleHits = report.getHitRules();
        sb.append("<section>\n<h2>Rule engine results <span class=\"muted\">(").append(ruleHits.size()).append(" rules matched)</span></h2>\n");
        sb.append("<input id=\"rule-filter\" class=\"filter\" placeholder=\"Filter rules…\" oninput=\"filterTable('rule-filter','rule-table')\">\n");
        sb.append("<table id=\"rule-table\" class=\"grid\">\n<thead><tr><th>Rule</th><th>Category</th><th>Severity</th><th>Matches</th></tr></thead><tbody>\n");
        for (ScanReport.RuleEntry r : ruleHits) {
            sb.append("<tr><td><b>").append(esc(r.name)).append("</b><div class=\"muted small\">").append(esc(r.description)).append("</div></td>");
            sb.append("<td>").append(esc(r.category)).append("</td>");
            sb.append("<td><span class=\"sev sev-").append(r.severity.name().toLowerCase()).append("\" style=\"--c:").append(r.severity.getColor()).append("\">").append(r.severity.getLabel()).append("</span></td>");
            sb.append("<td>");
            for (String m : r.matches) {
                sb.append("<div class=\"match\"><code>").append(esc(m)).append("</code></div>");
            }
            if (!r.validated.isEmpty()) {
                sb.append("<div class=\"muted small\">Offline:</div>");
                for (String v : r.validated) sb.append("<div class=\"match small\">").append(esc(v)).append("</div>");
            }
            if (!r.liveResults.isEmpty()) {
                sb.append("<div class=\"muted small\">Live:</div>");
                for (CredentialCheckResult c : r.liveResults) {
                    sb.append("<div class=\"match small\"><span class=\"status status-").append(c.getStatus().name().toLowerCase()).append("\">").append(c.getStatus()).append("</span> ").append(esc(c.getDetail())).append("</div>");
                }
            }
            sb.append("</td></tr>\n");
        }
        if (ruleHits.isEmpty()) {
            sb.append("<tr><td colspan=\"4\" class=\"empty\">No rule hits.</td></tr>\n");
        }
        sb.append("</tbody></table>\n</section>\n");

        sb.append("<footer>Generated by HeapDump Analyzer v").append(esc(report.getVersion())).append(" · Apache-2.0</footer>\n");
        sb.append("<script>\n").append(JS).append("\n</script>\n");
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private static String renderCategoryChart(Map<String, Integer> cats) {
        int max = cats.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"chart\">\n");
        for (Map.Entry<String, Integer> e : cats.entrySet()) {
            int pct = (int) Math.ceil(e.getValue() * 100.0 / max);
            sb.append("<div class=\"bar-row\"><div class=\"bar-label\">").append(esc(e.getKey())).append("</div>");
            sb.append("<div class=\"bar-track\"><div class=\"bar-fill\" style=\"width:").append(pct).append("%\"></div></div>");
            sb.append("<div class=\"bar-val\">").append(e.getValue()).append("</div></div>\n");
        }
        sb.append("</div>\n");
        return sb.toString();
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] u = {"KB", "MB", "GB", "TB"};
        double v = bytes;
        int i = -1;
        do { v /= 1024; i++; } while (v >= 1024 && i < u.length - 1);
        return String.format("%.1f %s", v, u[i]);
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final String CSS =
        ":root{--bg:#1e1e2e;--panel:#313244;--text:#cdd6f4;--muted:#a6adc8;--accent:#89b4fa;--crit:#f38ba8;--high:#fab387;--med:#f9e2af;--low:#a6e3a1;--info:#89dceb}\n" +
        "*{box-sizing:border-box}\n" +
        "body{margin:0;background:var(--bg);color:var(--text);font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;line-height:1.5;padding:24px}\n" +
        "header{max-width:1200px;margin:0 auto 24px}\n" +
        ".brand{font-size:28px;font-weight:700}.brand span{color:var(--accent);font-size:16px;font-weight:400}\n" +
        ".subtitle{color:var(--muted);font-style:italic;margin:4px 0 16px}\n" +
        ".meta{display:flex;flex-wrap:wrap;gap:24px;color:var(--muted);font-size:14px}.meta b{color:var(--text)}\n" +
        ".cards{display:flex;flex-wrap:wrap;gap:12px;max-width:1200px;margin:0 auto 8px}\n" +
        ".card{flex:1;min-width:140px;background:var(--panel);border-radius:12px;padding:18px;border-top:4px solid;text-align:center}\n" +
        ".card-num{font-size:38px;font-weight:700;line-height:1}\n" +
        ".card-label{text-transform:uppercase;letter-spacing:1px;font-size:12px;color:var(--muted);margin-top:6px}\n" +
        ".total{max-width:1200px;margin:0 auto 24px;color:var(--muted)}\n" +
        ".live-badge{background:var(--low);color:#1e1e2e;padding:2px 10px;border-radius:10px;font-weight:700;font-size:13px}\n" +
        "section{max-width:1200px;margin:0 auto 32px}\n" +
        "h2{font-size:20px;border-bottom:1px solid var(--panel);padding-bottom:8px;margin-bottom:16px}\n" +
        ".muted{color:var(--muted)}.small{font-size:12px}\n" +
        ".chart{display:flex;flex-direction:column;gap:8px}\n" +
        ".bar-row{display:flex;align-items:center;gap:12px}\n" +
        ".bar-label{width:140px;text-align:right;font-size:13px;color:var(--muted)}\n" +
        ".bar-track{flex:1;background:var(--panel);border-radius:6px;height:22px;overflow:hidden}\n" +
        ".bar-fill{height:100%;background:linear-gradient(90deg,var(--accent),#b4befe);border-radius:6px;min-width:2px}\n" +
        ".bar-val{width:40px;font-weight:700}\n" +
        ".filter{width:100%;padding:10px 14px;background:var(--panel);border:1px solid #45475a;border-radius:8px;color:var(--text);font-size:14px;margin-bottom:12px}\n" +
        ".grid{width:100%;border-collapse:collapse;font-size:13px}\n" +
        ".grid th{text-align:left;background:var(--panel);padding:10px 12px;font-weight:600;position:sticky;top:0}\n" +
        ".grid td{padding:10px 12px;border-top:1px solid #45475a;vertical-align:top}\n" +
        ".grid tr:hover{background:rgba(137,180,250,0.06)}\n" +
        ".grid pre{margin:0;white-space:pre-wrap;word-break:break-all;max-height:200px;overflow:auto;font-family:'JetBrains Mono',Consolas,monospace;font-size:12px}\n" +
        "code{font-family:'JetBrains Mono',Consolas,monospace;font-size:12px;word-break:break-all}\n" +
        ".sev{display:inline-block;padding:2px 10px;border-radius:10px;font-size:11px;font-weight:700;text-transform:uppercase;background:var(--c);color:#1e1e2e}\n" +
        ".status{display:inline-block;padding:1px 8px;border-radius:8px;font-size:11px;font-weight:700}\n" +
        ".status-live{background:var(--low);color:#1e1e2e}\n" +
        ".status-expired{background:var(--crit);color:#1e1e2e}\n" +
        ".status-unknown{background:var(--med);color:#1e1e2e}\n" +
        ".status-error{background:var(--high);color:#1e1e2e}\n" +
        ".match{margin:2px 0}.empty{text-align:center;color:var(--muted);padding:24px}\n" +
        "footer{max-width:1200px;margin:32px auto 0;text-align:center;color:var(--muted);font-size:12px;border-top:1px solid var(--panel);padding-top:16px}";

    private static final String JS =
        "function filterTable(inputId,tableId){var q=document.getElementById(inputId).value.toLowerCase();var rows=document.querySelectorAll('#'+tableId+' tbody tr');rows.forEach(function(r){r.style.display=r.textContent.toLowerCase().indexOf(q)>=0?'':'none'});}";
}
