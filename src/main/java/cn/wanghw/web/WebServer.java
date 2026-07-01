package cn.wanghw.web;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.Main;
import cn.wanghw.Severity;
import cn.wanghw.SpiderRegistry;
import cn.wanghw.report.HtmlReportRenderer;
import cn.wanghw.report.ScanReport;
import cn.wanghw.rule.Rule;
import cn.wanghw.rule.YamlRuleLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Zero-dependency Web UI server built on the JDK's built-in
 * {@code com.sun.net.httpserver.HttpServer} (no Javalin/Jetty required).
 *
 * <p>Serves a single-page app at {@code /} and a small JSON API under {@code /api/}.
 * The SPA lives in {@code resources/web/index.html} and is read from the classpath so it
 * works inside the shaded jar. A loaded heap dump is held in memory so repeated scans
 * don't re-parse the file.</p>
 */
public class WebServer {

    private final int port;
    private HttpServer server;
    private volatile File loadedFile;
    private volatile IHeapHolder loadedHeap;
    private volatile ScanReport lastReport;

    public WebServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/spiders", exchange -> handleJson(exchange, spidersJson()));
        server.createContext("/api/rules", exchange -> handleJson(exchange, rulesJson()));
        server.createContext("/api/load", new LoadHandler());
        server.createContext("/api/scan", new ScanHandler());
        server.createContext("/api/results", exchange -> {
            if (lastReport == null) {
                sendJson(exchange, 404, errorJson("no scan results yet"));
            } else {
                handleJson(exchange, reportJson(lastReport));
            }
        });
        server.createContext("/api/export", new ExportHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("==================================================");
        System.out.println("  HeapDump Analyzer v1.0 — Web UI");
        System.out.println("  Dig secrets out of JVM memory.");
        System.out.println("==================================================");
        System.out.println("  Listening on:  http://localhost:" + port);
        System.out.println("  Spiders: " + SpiderRegistry.getInstance().size());
        System.out.println("  Rules:    " + YamlRuleLoader.loadAll(null).size());
        System.out.println("--------------------------------------------------");
        System.out.println("  In the browser: load a heap dump, then 'Run scan'.");
        System.out.println("  Press Ctrl+C to stop.");
        System.out.println("==================================================");
        // Block the calling thread so the JVM stays alive serving requests.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ---- handlers ---------------------------------------------------------

    private final class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) path = "/index.html";
            String resource = "/web" + path;
            try (InputStream is = WebServer.class.getResourceAsStream(resource)) {
                if (is == null) {
                    sendText(exchange, 404, "Not found: " + path);
                    return;
                }
                byte[] body = is.readAllBytes();
                String mime = mimeFor(path);
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
        }
    }

    private final class LoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, errorJson("POST required"));
                return;
            }
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            Map<String, String> body = parseBody(exchange);
            String filePath = body.getOrDefault("file", params.get("file"));
            if (filePath == null || filePath.isBlank()) {
                sendJson(exchange, 400, errorJson("missing 'file' parameter"));
                return;
            }
            java.io.File f = new java.io.File(filePath);
            if (!f.exists() || !f.isFile()) {
                sendJson(exchange, 404, errorJson("file not found: " + filePath));
                return;
            }
            try {
                Main main = new Main();
                IHeapHolder holder = main.createHeapHolder(f);
                loadedFile = f;
                loadedHeap = holder;
                lastReport = null;
                JsonObject resp = new JsonObject();
                resp.addProperty("ok", true);
                resp.addProperty("file", f.getName());
                resp.addProperty("path", f.getAbsolutePath());
                resp.addProperty("size", f.length());
                sendJson(exchange, 200, resp.toString());
            } catch (Exception e) {
                sendJson(exchange, 500, errorJson("load failed: " + e.getMessage()));
            }
        }
    }

    private final class ScanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, errorJson("POST required"));
                return;
            }
            if (loadedHeap == null || loadedFile == null) {
                sendJson(exchange, 400, errorJson("no heap dump loaded — POST /api/load first"));
                return;
            }
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            Map<String, String> body = parseBody(exchange);
            String severity = body.getOrDefault("severity", params.getOrDefault("severity", "INFO")).toUpperCase();
            Severity minSev;
            try {
                minSev = Severity.valueOf(severity);
            } catch (IllegalArgumentException e) {
                minSev = Severity.INFO;
            }
            boolean validate = bool(body, params, "validate");
            boolean validateLive = bool(body, params, "validateLive");
            boolean parallel = bool(body, params, "parallel");
            boolean rulesOnly = bool(body, params, "rulesOnly");
            int threads = intVal(body, params, "threads", 0);

            Main main = new Main();
            main.setWebScanOptions(validate, validateLive, parallel, rulesOnly, threads);
            ScanReport report = main.scan(loadedFile, loadedHeap, minSev);
            lastReport = report;
            sendJson(exchange, 200, reportJson(report));
        }
    }

    private final class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (lastReport == null) {
                sendText(exchange, 400, "No scan results to export. Run a scan first.");
                return;
            }
            String fmt = param(exchange, "format", "html").toLowerCase();
            switch (fmt) {
                case "json" -> {
                    byte[] body = reportJson(lastReport).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.getResponseHeaders().set("Content-Disposition",
                            "attachment; filename=\"" + safeName(lastReport) + ".json\"");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                }
                case "csv" -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("type,name,category,severity,data\n");
                    for (ScanReport.SpiderEntry s : lastReport.getHitSpiders()) {
                        sb.append("spider,").append(s.name).append(",").append(s.category)
                          .append(",").append(s.severity.name()).append(",\"")
                          .append(s.data.replace("\"", "\"\"")).append("\"\n");
                    }
                    byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                    exchange.getResponseHeaders().set("Content-Disposition",
                            "attachment; filename=\"" + safeName(lastReport) + ".csv\"");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                }
                default -> {
                    byte[] body = HtmlReportRenderer.render(lastReport).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.getResponseHeaders().set("Content-Disposition",
                            "attachment; filename=\"" + safeName(lastReport) + ".html\"");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                }
            }
        }
    }

    // ---- JSON builders ----------------------------------------------------

    private String spidersJson() {
        List<ISpider> spiders = SpiderRegistry.getInstance().getSpiders();
        Map<String, List<ISpider>> byCat = SpiderRegistry.getInstance().getByCategory();
        JsonObject root = new JsonObject();
        root.addProperty("total", spiders.size());
        JsonObject cats = new JsonObject();
        for (Map.Entry<String, List<ISpider>> e : byCat.entrySet()) {
            cats.addProperty(e.getKey(), e.getValue().size());
        }
        root.add("categories", cats);
        root.add("spiders", new Gson().toJsonTree(spiders.stream().map(s -> {
            JsonObject o = new JsonObject();
            o.addProperty("name", s.getName());
            o.addProperty("category", s.getCategory());
            o.addProperty("severity", s.getSeverity().name());
            o.addProperty("description", s.getDescription());
            return o;
        }).toList()));
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private String rulesJson() {
        List<Rule> rules = YamlRuleLoader.loadAll(null);
        JsonObject root = new JsonObject();
        root.addProperty("total", rules.size());
        root.add("rules", new Gson().toJsonTree(rules.stream().map(r -> {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.getId());
            o.addProperty("name", r.getName());
            o.addProperty("category", r.getCategory());
            o.addProperty("severity", r.getSeverity().name());
            o.addProperty("description", r.getDescription());
            return o;
        }).toList()));
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private String reportJson(ScanReport report) {
        JsonObject root = new JsonObject();
        root.addProperty("file", report.getFileName());
        root.addProperty("scanTime", report.getScanTime());
        root.addProperty("version", report.getVersion());
        root.addProperty("spiderCount", report.getSpiderCount());
        root.addProperty("ruleCount", report.getRuleCount());
        root.addProperty("liveCredentials", report.liveCount());

        JsonObject sev = new JsonObject();
        for (Map.Entry<Severity, Integer> e : report.severityCounts().entrySet()) {
            sev.addProperty(e.getKey().name(), e.getValue());
        }
        root.add("severity", sev);
        root.add("categories", new Gson().toJsonTree(report.categoryCounts()));

        root.add("spiders", new Gson().toJsonTree(report.getHitSpiders()));
        root.add("rules", new Gson().toJsonTree(report.getHitRules()));
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    // ---- helpers ----------------------------------------------------------

    private void handleJson(HttpExchange exchange, String json) throws IOException {
        sendJson(exchange, 200, json);
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void sendText(HttpExchange exchange, int code, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String errorJson(String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("error", msg);
        return o.toString();
    }

    private String mimeFor(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> m = new HashMap<>();
        if (query == null || query.isBlank()) return m;
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) m.put(urlDecode(pair.substring(0, i)), urlDecode(pair.substring(i + 1)));
            else m.put(urlDecode(pair), "");
        }
        return m;
    }

    private Map<String, String> parseBody(HttpExchange exchange) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        String body = new String(raw, StandardCharsets.UTF_8).trim();
        if (body.isEmpty()) return new HashMap<>();
        if (body.startsWith("{")) {
            // simple JSON { "file": "..." } → parse manually
            Map<String, String> m = new HashMap<>();
            try {
                JsonObject o = new Gson().fromJson(body, JsonObject.class);
                for (String k : o.keySet()) {
                    if (o.get(k).isJsonPrimitive()) m.put(k, o.get(k).getAsString());
                }
            } catch (Exception ignored) {}
            return m;
        }
        return parseQuery(body);
    }

    private String param(HttpExchange exchange, String key, String def) {
        Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
        return q.getOrDefault(key, def);
    }

    private boolean bool(Map<String, String> body, Map<String, String> params, String key) {
        String v = body.getOrDefault(key, params.get(key));
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
    }

    private int intVal(Map<String, String> body, Map<String, String> params, String key, int def) {
        String v = body.getOrDefault(key, params.get(key));
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    private String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private String safeName(ScanReport r) {
        String n = r.getFileName();
        return n == null ? "heapdump-report" : n.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
