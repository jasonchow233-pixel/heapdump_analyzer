package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Extract Couchbase credentials from couchbase-java SDK
 * com.couchbase.client.java.env.CouchbaseEnvironment
 */
public class CouchbaseConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "CouchbaseConfig";
    }

    @Override
    public String getCategory() {
        return "database";
    }

    @Override
    public String getDescription() {
        return "Extract Couchbase connection credentials (hosts, bucket, password) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "com.couchbase.client.java.env.DefaultCouchbaseEnvironment",
                "com.couchbase.client.core.env.CoreEnvironment"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    HashMap<String, String> allFields = heapHolder.getAllFieldValues(instance);
                    for (Map.Entry<String, String> entry : allFields.entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue();
                        if (val != null && !val.isEmpty() &&
                                (key.contains("host") || key.contains("bucket") ||
                                 key.contains("pass") || key.contains("user") ||
                                 key.contains("port") || key.contains("seed"))) {
                            fields.put(key, val);
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
