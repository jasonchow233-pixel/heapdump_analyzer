package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract New Relic credentials from newrelic-api / agent
 * com.newrelic.api.agent.Config, com.newrelic.telemetry.TelemetryClient
 */
public class NewRelicConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "NewRelicConfig";
    }

    @Override
    public String getCategory() {
        return "cloud";
    }

    @Override
    public String getDescription() {
        return "Extract New Relic license keys and configuration from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "com.newrelic.telemetry.TelemetryClient",
                "com.newrelic.api.agent.Config"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"licenseKey", "apiKey", "insertKey",
                            "host", "port", "appName", "auditMode"};
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
