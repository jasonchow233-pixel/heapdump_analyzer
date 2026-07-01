package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Twilio credentials from twilio-java SDK
 * com.twilio.Twilio, com.twilio.http.TwilioRestClient
 */
public class TwilioConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "TwilioConfig";
    }

    @Override
    public String getCategory() {
        return "cloud";
    }

    @Override
    public String getDescription() {
        return "Extract Twilio API credentials (account SID, auth token) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "com.twilio.http.TwilioRestClient",
                "com.twilio.Twilio"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"accountSid", "authToken", "username", "password",
                            "apiKeySid", "apiKeySecret"};
                    for (String fn : fieldNames) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
                    }
                    // Try nested auth object
                    try {
                        Object auth = heapHolder.getFieldValue(instance, "auth");
                        if (auth != null) {
                            String sid = heapHolder.getFieldStringValue(auth, "accountSid");
                            String token = heapHolder.getFieldStringValue(auth, "authToken");
                            if (sid != null) fields.put("auth.accountSid", sid);
                            if (token != null) fields.put("auth.authToken", token);
                        }
                    } catch (Exception ignored) {}
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
