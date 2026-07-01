package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract SendGrid credentials from sendgrid-java SDK
 * com.sendgrid.SendGrid
 */
public class SendGridConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "SendGridConfig";
    }

    @Override
    public String getCategory() {
        return "cloud";
    }

    @Override
    public String getDescription() {
        return "Extract SendGrid API keys from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("com.sendgrid.SendGrid");
            if (clazz != null) {
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String apiKey = heapHolder.getFieldStringValue(instance, "apiKey");
                    if (apiKey != null && !apiKey.isEmpty()) {
                        fields.put("apiKey", apiKey);
                    }
                    String host = heapHolder.getFieldStringValue(instance, "host");
                    if (host != null && !host.isEmpty()) {
                        fields.put("host", host);
                    }
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {}
        return result.toString();
    }
}
