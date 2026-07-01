package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Slack credentials from slack-api-client / bolt-java
 * com.slack.api.Slack, com.slack.api.methods.MethodsClient
 */
public class SlackConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "SlackConfig";
    }

    @Override
    public String getCategory() {
        return "cloud";
    }

    @Override
    public String getDescription() {
        return "Extract Slack API tokens and bot tokens from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "com.slack.api.Slack",
                "com.slack.api.methods.impl.MethodsClientImpl"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"token", "botToken", "appToken",
                            "baseUrl", "executor"};
                    for (String fn : fieldNames) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
                    }
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
