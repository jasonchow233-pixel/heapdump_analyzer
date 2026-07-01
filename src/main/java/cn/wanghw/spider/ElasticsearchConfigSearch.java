package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Extract Elasticsearch credentials from rest-high-level-client / new Java client
 * org.elasticsearch.client.RestClient
 */
public class ElasticsearchConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "ElasticsearchConfig";
    }

    @Override
    public String getCategory() {
        return "database";
    }

    @Override
    public String getDescription() {
        return "Extract Elasticsearch connection credentials (hosts, auth) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "org.elasticsearch.client.RestClient",
                "org.elasticsearch.client.RestHighLevelClient",
                "co.elastic.clients.elasticsearch.ElasticsearchClient"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    // Try to get all field values
                    HashMap<String, String> allFields = heapHolder.getAllFieldValues(instance);
                    for (Map.Entry<String, String> entry : allFields.entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue();
                        if (val != null && !val.isEmpty() &&
                                (key.contains("host") || key.contains("port") ||
                                 key.contains("url") || key.contains("auth") ||
                                 key.contains("user") || key.contains("pass") ||
                                 key.contains("key") || key.contains("token"))) {
                            fields.put(key, val);
                        }
                    }
                    if (!fields.isEmpty()) {
                        result.append(className).append(": ")
                              .append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
