package cn.wanghw.rule.validator;

import cn.wanghw.rule.CredentialCheckResult;
import cn.wanghw.rule.CredentialStatus;
import cn.wanghw.rule.LiveValidator;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub personal access / fine-grained token validator.
 *
 * <p>{@link #validate(List)} is offline: it classifies the token type by prefix
 * (classic {@code ghp_}, fine-grained {@code github_pat_}, legacy {@code gh[opsu]_},
 * OAuth {@code gho_}, etc.). No network.</p>
 *
 * <p>{@link #validateLive(List)} calls {@code GET https://api.github.com/user} with the
 * token and tags it LIVE / EXPIRED based on the HTTP response.</p>
 *
 * <p>Pattern source: {@code github-token.yml}.</p>
 */
public class GitHubTokenValidator implements LiveValidator {

    private static final int TIMEOUT_MS = 5000;

    @Override
    public String getName() {
        return "GitHubTokenValidator";
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
                URL url = new URL("https://api.github.com/user");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "token " + token);
                conn.setRequestProperty("User-Agent", "heapdump-analyzer");
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                try {
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        String body = readAll(conn);
                        out.add(new CredentialCheckResult(token, CredentialStatus.LIVE, "login=" + extractLogin(body)));
                    } else if (code == 401 || code == 403) {
                        out.add(new CredentialCheckResult(token, CredentialStatus.EXPIRED, "HTTP " + code));
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
        if (token.startsWith("ghp_")) return "classic PAT";
        if (token.startsWith("github_pat_")) return "fine-grained PAT";
        if (token.startsWith("gho_")) return "OAuth token";
        if (token.startsWith("ghs_")) return "server-to-server";
        if (token.startsWith("ghu_")) return "user-to-server";
        if (token.startsWith("ghr_")) return "refresh token";
        if (token.startsWith("gh[opsu]_".substring(0, 2))) return "legacy token";
        return "unknown";
    }

    private boolean isPlausible(String token) {
        if (token.startsWith("ghp_") || token.startsWith("github_pat_")) return token.length() >= 36;
        return token.length() >= 20 && token.length() <= 80;
    }

    private String readAll(HttpsURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractLogin(String json) {
        try {
            int idx = json.indexOf("\"login\":\"");
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
