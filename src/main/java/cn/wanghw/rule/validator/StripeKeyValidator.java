package cn.wanghw.rule.validator;

import cn.wanghw.rule.CredentialCheckResult;
import cn.wanghw.rule.CredentialStatus;
import cn.wanghw.rule.LiveValidator;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Stripe API key validator.
 *
 * <p>{@link #validate(List)} is offline: classifies the key by prefix
 * ({@code sk_live_}/{@code sk_test_}/{@code pk_live_}/{@code pk_test_}/{@code rk_live_}).
 * No network.</p>
 *
 * <p>{@link #validateLive(List)} calls {@code GET https://api.stripe.com/v1/charges?limit=1}
 * with HTTP Basic auth (key as username). Only secret keys ({@code sk_}/{@code rk_}) are
 * verifiable server-side; publishable keys return UNKNOWN.</p>
 */
public class StripeKeyValidator implements LiveValidator {

    private static final int TIMEOUT_MS = 5000;

    @Override
    public String getName() {
        return "StripeKeyValidator";
    }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> out = new ArrayList<>(candidates.size());
        for (String key : candidates) {
            out.add(key + " [TYPE: " + classify(key) + " | FORMAT: " + (isPlausible(key) ? "OK" : "SUSPECT") + "]");
        }
        return out;
    }

    @Override
    public List<CredentialCheckResult> validateLive(List<String> candidates) {
        List<CredentialCheckResult> out = new ArrayList<>(candidates.size());
        for (String key : candidates) {
            if (!key.startsWith("sk_") && !key.startsWith("rk_")) {
                out.add(new CredentialCheckResult(key, CredentialStatus.UNKNOWN,
                        "publishable key, not verifiable server-side"));
                continue;
            }
            try {
                URL url = new URL("https://api.stripe.com/v1/charges?limit=1");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                String auth = java.util.Base64.getEncoder().encodeToString((key + ":").getBytes());
                conn.setRequestProperty("Authorization", "Basic " + auth);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                try {
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        out.add(new CredentialCheckResult(key, CredentialStatus.LIVE, classify(key) + " works"));
                    } else if (code == 401) {
                        out.add(new CredentialCheckResult(key, CredentialStatus.EXPIRED, "unauthorized"));
                    } else {
                        out.add(new CredentialCheckResult(key, CredentialStatus.UNKNOWN, "HTTP " + code));
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                out.add(new CredentialCheckResult(key, CredentialStatus.ERROR, e.getMessage()));
            }
        }
        return out;
    }

    private String classify(String key) {
        if (key.startsWith("sk_live_")) return "LIVE secret";
        if (key.startsWith("sk_test_")) return "TEST secret";
        if (key.startsWith("rk_live_")) return "LIVE restricted";
        if (key.startsWith("rk_test_")) return "TEST restricted";
        if (key.startsWith("pk_live_")) return "LIVE publishable";
        if (key.startsWith("pk_test_")) return "TEST publishable";
        return "UNKNOWN";
    }

    private boolean isPlausible(String key) {
        return key.length() >= 24;
    }
}
