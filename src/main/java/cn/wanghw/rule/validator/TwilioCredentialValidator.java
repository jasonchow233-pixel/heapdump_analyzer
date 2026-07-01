package cn.wanghw.rule.validator;

import cn.wanghw.rule.Validator;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates Twilio credentials by calling Accounts API.
 * Format: AccountSID:AuthToken
 */
public class TwilioCredentialValidator implements Validator {
    @Override
    public String getName() { return "TwilioCredentialValidator"; }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> valid = new ArrayList<>();
        for (String cred : candidates) {
            try {
                String[] parts = cred.split("[:|]");
                if (parts.length < 2) {
                    valid.add(cred + " [INVALID: expected AccountSID:AuthToken format]");
                    continue;
                }
                String sid = parts[0].trim();
                String token = parts[1].trim();
                URL url = new URL("https://api.twilio.com/2010-04-01/Accounts/" + sid + ".json");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                String auth = java.util.Base64.getEncoder().encodeToString((sid + ":" + token).getBytes());
                conn.setRequestProperty("Authorization", "Basic " + auth);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    valid.add(cred + " [VALID: " + extractFriendlyName(response.toString()) + "]");
                } else if (code == 401) {
                    valid.add(cred + " [INVALID: unauthorized]");
                } else {
                    valid.add(cred + " [CHECK_FAILED: HTTP " + code + "]");
                }
            } catch (Exception e) {
                valid.add(cred + " [ERROR: " + e.getMessage() + "]");
            }
        }
        return valid;
    }

    private String extractFriendlyName(String json) {
        try {
            int idx = json.indexOf("\"friendly_name\":\"");
            if (idx >= 0) {
                int start = idx + 17;
                int end = json.indexOf("\"", start);
                return "name=" + json.substring(start, end);
            }
        } catch (Exception ignored) {}
        return "account active";
    }
}
