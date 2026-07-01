package cn.wanghw.rule.validator;

import cn.wanghw.rule.Validator;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Validates JWT tokens by decoding the header and payload without signature verification.
 * This is a local-only validator that checks token structure and expiry.
 */
public class JwkSetValidator implements Validator {
    @Override
    public String getName() { return "JwkSetValidator"; }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> valid = new ArrayList<>();
        for (String token : candidates) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length < 2) {
                    valid.add(token + " [INVALID: not a JWT]");
                    continue;
                }
                // Decode header
                String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
                // Decode payload
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                // Check expiry
                String status = checkExpiry(payloadJson);
                valid.add(token + " [JWT_HEADER: " + headerJson
                        + " | PAYLOAD: " + payloadJson + " | " + status + "]");
            } catch (Exception e) {
                valid.add(token + " [INVALID: decode failed - " + e.getMessage() + "]");
            }
        }
        return valid;
    }

    private String checkExpiry(String payloadJson) {
        try {
            int expIdx = payloadJson.indexOf("\"exp\"");
            if (expIdx < 0) return "NO_EXP";
            int colonIdx = payloadJson.indexOf(":", expIdx);
            int commaIdx = payloadJson.indexOf(",", colonIdx);
            int braceIdx = payloadJson.indexOf("}", colonIdx);
            int endIdx = Math.min(commaIdx > 0 ? commaIdx : Integer.MAX_VALUE,
                    braceIdx > 0 ? braceIdx : Integer.MAX_VALUE);
            String expStr = payloadJson.substring(colonIdx + 1, endIdx).trim();
            long exp = Long.parseLong(expStr);
            long now = System.currentTimeMillis() / 1000;
            if (exp < now) {
                return "EXPIRED";
            } else {
                return "VALID (expires in " + ((exp - now) / 3600) + "h)";
            }
        } catch (Exception e) {
            return "EXPIRY_CHECK_FAILED";
        }
    }
}
