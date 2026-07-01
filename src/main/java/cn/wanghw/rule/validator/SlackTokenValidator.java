package cn.wanghw.rule.validator;

import cn.wanghw.rule.CredentialCheckResult;
import cn.wanghw.rule.CredentialStatus;
import cn.wanghw.rule.LiveValidator;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Slack token validator.
 *
 * <p>{@link #validate(List)} is offline: classifies by prefix
 * ({@code xoxb-} bot, {@code xoxp-} user, {@code xoxa-} app, {@code xoxr-} refresh,
 * {@code xapp-} app-level). No network.</p>
 *
 * <p>{@link #validateLive(List)} calls {@code POST https://slack.com/api/auth.test}
 * with {@code Authorization: Bearer <token>}. {@code "ok":true} → LIVE, otherwise EXPIRED.</p>
 */
public class SlackTokenValidator implements LiveValidator {

    private static final int TIMEOUT_MS = 5000;

    @Override
    public String getName() {
        return "SlackTokenValidator";
    }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> out = new ArrayList<>(candidates.size());
        for (String token : candidates) {
            out.add(token + " [TYPE: " + classify(token) + " | FORMAT: " + (isPlausible(token) ? "OK" : "SUSPECT") + "]");
        }
        return out;
    }

    @Override
    public List<CredentialCheckResult> validateLive(List<String> candidates) {
        List<CredentialCheckResult> out = new ArrayList<>(candidates.size());
        for (String token : candidates) {
            try {
                URL url = new URL("https://slack.com/api/auth.test");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.getOutputStream().write("token=".getBytes(StandardCharsets.UTF_8));
                try {
                    int code = conn.getResponseCode();
                    String body = readAll(conn);
                    if (code == 200 && body.contains("\"ok\":true")) {
                        out.add(new CredentialCheckResult(token, CredentialStatus.LIVE, extractTeam(body)));
                    } else if (code == 200) {
                        out.add(new CredentialCheckResult(token, CredentialStatus.EXPIRED, extractError(body)));
                    } else {
                        out.add(new CredentialCheckResult(token, CredentialStatus.UNKNOWN, "HTTP " + code));
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                out.add(new CredentialCheckResult(token, CredentialStatus.ERROR, e.getMessage()));
            }
        }
        return out;
    }

    private String classify(String token) {
        if (token.startsWith("xoxb-")) return "bot token";
        if (token.startsWith("xoxp-")) return "user token";
        if (token.startsWith("xoxa-")) return "app token";
        if (token.startsWith("xoxr-")) return "refresh token";
        if (token.startsWith("xapp-")) return "app-level token";
        return "unknown";
    }

    private boolean isPlausible(String token) {
        return token.length() >= 24;
    }

    private String readAll(HttpsURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractTeam(String json) {
        try {
            int idx = json.indexOf("\"team\":\"");
            if (idx >= 0) {
                int start = idx + 8;
                int end = json.indexOf("\"", start);
                return "team=" + json.substring(start, end);
            }
        } catch (Exception ignored) {
        }
        return "team=unknown";
    }

    private String extractError(String json) {
        try {
            int idx = json.indexOf("\"error\":\"");
            if (idx >= 0) {
                int start = idx + 9;
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
