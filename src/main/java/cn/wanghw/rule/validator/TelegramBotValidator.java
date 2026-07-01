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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Telegram Bot token validator.
 *
 * <p>{@link #validate(List)} is offline: checks the {@code <bot_id>:<secret>} shape and
 * extracts the bot id. No network.</p>
 *
 * <p>{@link #validateLive(List)} calls {@code GET https://api.telegram.org/bot<TOKEN>/getMe}.
 * HTTP 200 with {@code "ok":true} → LIVE; 401 / {@code "ok":false} → EXPIRED.</p>
 *
 * <p>Pattern source: {@code telegram-bot-token.yml}.</p>
 */
public class TelegramBotValidator implements LiveValidator {

    private static final int TIMEOUT_MS = 5000;
    private static final Pattern SHAPE = Pattern.compile("([0-9]{8,10}):[A-Za-z0-9_\\-]{35}");

    @Override
    public String getName() {
        return "TelegramBotValidator";
    }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> out = new ArrayList<>(candidates.size());
        for (String token : candidates) {
            Matcher m = SHAPE.matcher(token);
            String botId = m.find() ? m.group(1) : "unknown";
            out.add(token + " [TYPE: Telegram Bot Token | BOT_ID: " + botId + " | FORMAT: OK]");
        }
        return out;
    }

    @Override
    public List<CredentialCheckResult> validateLive(List<String> candidates) {
        List<CredentialCheckResult> out = new ArrayList<>(candidates.size());
        for (String token : candidates) {
            try {
                URL url = new URL("https://api.telegram.org/bot" + token + "/getMe");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                try {
                    int code = conn.getResponseCode();
                    String body = readAll(conn);
                    if (code == 200 && body.contains("\"ok\":true")) {
                        out.add(new CredentialCheckResult(token, CredentialStatus.LIVE, "bot=" + extractUsername(body)));
                    } else if (code == 401 || (code == 200 && body.contains("\"ok\":false"))) {
                        out.add(new CredentialCheckResult(token, CredentialStatus.EXPIRED, "unauthorized"));
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

    private String readAll(HttpsURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractUsername(String json) {
        try {
            int idx = json.indexOf("\"username\":\"");
            if (idx >= 0) {
                int start = idx + 12;
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
