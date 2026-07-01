package cn.wanghw.rule.validator;

import cn.wanghw.rule.Validator;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates SendGrid API keys by calling /user/profile API.
 * Pattern: SG.xxx
 */
public class SendGridKeyValidator implements Validator {
    @Override
    public String getName() { return "SendGridKeyValidator"; }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> valid = new ArrayList<>();
        for (String key : candidates) {
            try {
                URL url = new URL("https://api.sendgrid.com/v3/user/profile");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + key);
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
                    valid.add(key + " [VALID: SendGrid key active]");
                } else if (code == 401) {
                    valid.add(key + " [INVALID: unauthorized]");
                } else {
                    valid.add(key + " [CHECK_FAILED: HTTP " + code + "]");
                }
            } catch (Exception e) {
                valid.add(key + " [ERROR: " + e.getMessage() + "]");
            }
        }
        return valid;
    }
}
