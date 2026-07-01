package cn.wanghw.rule.validator;

import cn.wanghw.rule.Validator;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates Firebase project configurations by checking if Realtime Database or Firestore endpoints are accessible.
 * This does NOT validate API keys (Firebase API keys are not secret), but checks for misconfigured security rules.
 */
public class FirebaseValidator implements Validator {
    @Override
    public String getName() { return "FirebaseValidator"; }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> valid = new ArrayList<>();
        for (String config : candidates) {
            try {
                // Extract project ID from config string
                String projectId = extractValue(config, "projectId");
                String databaseUrl = extractValue(config, "databaseUrl");

                if (projectId != null) {
                    // Check Realtime Database open access
                    String dbCheck = checkRealtimeDb(projectId, databaseUrl);
                    // Check Firestore open access
                    String firestoreCheck = checkFirestore(projectId);
                    valid.add(config + " [" + dbCheck + " | " + firestoreCheck + "]");
                } else {
                    valid.add(config + " [NO_PROJECT_ID: cannot validate]");
                }
            } catch (Exception e) {
                valid.add(config + " [ERROR: " + e.getMessage() + "]");
            }
        }
        return valid;
    }

    private String checkRealtimeDb(String projectId, String databaseUrl) {
        try {
            String url = databaseUrl != null ? databaseUrl :
                    "https://" + projectId + "-default-rtdb.firebaseio.com/.json";
            if (!url.endsWith(".json")) url = url + "/.json";
            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try {
                int code = conn.getResponseCode();
                if (code == 200) {
                    return "RTDB_OPEN_READ (MISCONFIGURED)";
                } else if (code == 401 || code == 403) {
                    return "RTDB_PROTECTED";
                }
                return "RTDB_HTTP_" + code;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return "RTDB_UNREACHABLE";
        }
    }

    private String checkFirestore(String projectId) {
        try {
            String url = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try {
                int code = conn.getResponseCode();
                if (code == 200) {
                    return "FIRESTORE_OPEN_READ (MISCONFIGURED)";
                } else if (code == 401 || code == 403 || code == 404) {
                    return "FIRESTORE_PROTECTED";
                }
                return "FIRESTORE_HTTP_" + code;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return "FIRESTORE_UNREACHABLE";
        }
    }

    private String extractValue(String config, String key) {
        try {
            int idx = config.indexOf(key + "=");
            if (idx < 0) idx = config.indexOf(key + ": ");
            if (idx < 0) return null;
            int start = config.indexOf("=", idx) + 1;
            if (start == 0) start = config.indexOf(":", idx) + 2;
            int end = config.indexOf(",", start);
            if (end < 0) end = config.indexOf("}", start);
            if (end < 0) end = config.length();
            return config.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
