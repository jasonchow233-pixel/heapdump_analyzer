package cn.wanghw.rule.validator;

import cn.wanghw.rule.Validator;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates Google Cloud API keys by making a test call.
 * Google API keys themselves are not secrets, but we can check if they have restricted usage.
 */
public class GcpKeyValidator implements Validator {
    @Override
    public String getName() { return "GcpKeyValidator"; }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> valid = new ArrayList<>();
        for (String key : candidates) {
            try {
                // Test with a public API call (geocoding)
                URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?address=test&key=" + key);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String resp = response.toString();
                if (resp.contains("\"status\":\"OK\"") || resp.contains("\"status\":\"ZERO_RESULTS\"")) {
                    valid.add(key + " [VALID: Google API key is active and unrestricted]");
                } else if (resp.contains("\"status\":\"REQUEST_DENIED\"")) {
                    String msg = extractErrorMessage(resp);
                    valid.add(key + " [RESTRICTED: " + msg + "]");
                } else {
                    valid.add(key + " [CHECK_FAILED: " + truncate(resp, 100) + "]");
                }
            } catch (Exception e) {
                valid.add(key + " [ERROR: " + e.getMessage() + "]");
            }
        }
        return valid;
    }

    private String extractErrorMessage(String json) {
        try {
            int idx = json.indexOf("\"error_message\":\"");
            if (idx >= 0) {
                int start = idx + 16;
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            }
        } catch (Exception ignored) {}
        return "unknown restriction";
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
