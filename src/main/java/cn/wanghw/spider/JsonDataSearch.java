package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 搜索JSON格式的数据，包括：
 * - JSON字符串（以 { 或 [ 开头的字符串）
 * - Jackson JsonNode/ObjectNode/ArrayNode
 * - Gson JsonObject/JsonArray
 * - JSON.org JSONObject/JSONArray
 * - Fastjson JSONObject/JSONArray
 */
public class JsonDataSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDataSearch.class);

    // JSON格式识别的正则表达式
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
        "^\\s*\\{[\\s\\S]*\\}\\s*$",
        Pattern.MULTILINE
    );

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
        "^\\s*\\[[\\s\\S]*\\]\\s*$",
        Pattern.MULTILINE
    );

    // 简单的JSON检测（快速匹配）
    private static final Pattern QUICK_JSON_PATTERN = Pattern.compile(
        "^\\s*[\\{\\[].*[\\}\\]]\\s*$",
        Pattern.DOTALL
    );

    @Override
    public String getName() {
        return "JsonData";
    }

    @Override
    public String getCategory() {
        return "data";
    }

    @Override
    public String getDescription() {
        return "Extract JSON format data from strings and JSON library objects (Jackson, Gson, Fastjson, etc.)";
    }

    @Override
    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.MEDIUM;
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();

        // 1. 搜索JSON字符串
        result.append(sniffJsonStrings(heapHolder));

        // 2. 搜索Jackson JsonNode
        result.append(sniffJacksonNodes(heapHolder));

        // 3. 搜索Gson对象
        result.append(sniffGsonObjects(heapHolder));

        // 4. 搜索Fastjson对象
        result.append(sniffFastjsonObjects(heapHolder));

        // 5. 搜索JSON.org对象
        result.append(sniffJsonOrgObjects(heapHolder));

        return result.length() == 0 ? null : result.toString();
    }

    /**
     * 搜索JSON格式的字符串
     */
    private String sniffJsonStrings(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        Set<String> seen = new HashSet<>();

        try {
            Object stringClass = heapHolder.findClass("java.lang.String");
            if (stringClass == null) return result.toString();

            int count = 0;
            int maxResults = 500;  // 限制结果数量

            for (Object instance : heapHolder.getInstances(stringClass)) {
                if (count >= maxResults) break;
                if (Thread.currentThread().isInterrupted()) break;

                try {
                    String str = heapHolder.toString(instance);
                    if (str == null || str.length() < 10 || str.length() > 10000) continue;

                    // 快速检测是否可能是JSON
                    if (!QUICK_JSON_PATTERN.matcher(str).matches()) continue;

                    // 更精确的JSON格式验证
                    if (isValidJson(str)) {
                        if (seen.contains(str)) continue;
                        seen.add(str);

                        String jsonType = str.trim().startsWith("{") ? "JSONObject" : "JSONArray";
                        HashMap<String, String> fields = new HashMap<>();
                        fields.put("type", jsonType);
                        fields.put("length", String.valueOf(str.length()));

                        // 如果JSON不太长，直接显示
                        if (str.length() <= 200) {
                            fields.put("content", str);
                        } else {
                            fields.put("content", str.substring(0, 200) + "...");
                            fields.put("truncated", "true");
                        }

                        result.append("[JsonString] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                        count++;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to sniff JSON strings: {}", e.getMessage());
        }

        return result.toString();
    }

    /**
     * 搜索Jackson JsonNode相关对象
     */
    private String sniffJacksonNodes(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "com.fasterxml.jackson.databind.JsonNode",
            "com.fasterxml.jackson.databind.node.ObjectNode",
            "com.fasterxml.jackson.databind.node.ArrayNode",
            "com.fasterxml.jackson.databind.node.TextNode",
            "com.fasterxml.jackson.databind.node.IntNode",
            "com.fasterxml.jackson.databind.node.BooleanNode"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    fields.put("library", "Jackson");
                    fields.put("nodeType", className.substring(className.lastIndexOf('.') + 1));

                    // 尝试获取JSON内容
                    try {
                        String jsonStr = heapHolder.toString(instance);
                        if (jsonStr != null && !jsonStr.isEmpty()) {
                            if (jsonStr.length() <= 500) {
                                fields.put("content", jsonStr);
                            } else {
                                fields.put("content", jsonStr.substring(0, 500) + "...");
                                fields.put("truncated", "true");
                            }
                        }
                    } catch (Exception ignored) {}

                    // Node type
                    try {
                        Object nodeType = heapHolder.getFieldValue(instance, "NODE_TYPE");
                        if (nodeType != null) {
                            fields.put("jsonNodeType", nodeType.toString());
                        }
                    } catch (Exception ignored) {}

                    result.append("[JacksonJson] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }

    /**
     * 搜索Gson JsonElement相关对象
     */
    private String sniffGsonObjects(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "com.google.gson.JsonObject",
            "com.google.gson.JsonArray",
            "com.google.gson.JsonPrimitive",
            "com.google.gson.JsonNull"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    fields.put("library", "Gson");
                    fields.put("nodeType", className.substring(className.lastIndexOf('.') + 1));

                    // 尝试获取JSON内容
                    try {
                        String jsonStr = heapHolder.toString(instance);
                        if (jsonStr != null && !jsonStr.isEmpty()) {
                            if (jsonStr.length() <= 500) {
                                fields.put("content", jsonStr);
                            } else {
                                fields.put("content", jsonStr.substring(0, 500) + "...");
                                fields.put("truncated", "true");
                            }
                        }
                    } catch (Exception ignored) {}

                    // Members (for JsonObject)
                    if (className.contains("JsonObject")) {
                        try {
                            Object members = heapHolder.getFieldValue(instance, "members");
                            if (members != null) {
                                fields.put("fieldCount", "present");
                            }
                        } catch (Exception ignored) {}
                    }

                    // Elements (for JsonArray)
                    if (className.contains("JsonArray")) {
                        try {
                            Object elements = heapHolder.getFieldValue(instance, "elements");
                            if (elements != null) {
                                fields.put("elementCount", "present");
                            }
                        } catch (Exception ignored) {}
                    }

                    result.append("[GsonJson] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }

    /**
     * 搜索Fastjson JSONObject/JSONArray
     */
    private String sniffFastjsonObjects(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "com.alibaba.fastjson.JSONObject",
            "com.alibaba.fastjson.JSONArray"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    fields.put("library", "Fastjson");
                    fields.put("nodeType", className.substring(className.lastIndexOf('.') + 1));

                    // 尝试获取JSON内容
                    try {
                        String jsonStr = heapHolder.toString(instance);
                        if (jsonStr != null && !jsonStr.isEmpty()) {
                            if (jsonStr.length() <= 500) {
                                fields.put("content", jsonStr);
                            } else {
                                fields.put("content", jsonStr.substring(0, 500) + "...");
                                fields.put("truncated", "true");
                            }
                        }
                    } catch (Exception ignored) {}

                    // Map (internal map)
                    try {
                        Object map = heapHolder.getFieldValue(instance, "map");
                        if (map != null) {
                            HashMap<String, String> mapContents = heapHolder.arrayDump(heapHolder.getMap(map));
                            if (mapContents != null) {
                                fields.put("size", String.valueOf(mapContents.size()));
                            }
                        }
                    } catch (Exception ignored) {}

                    result.append("[FastjsonJson] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }

    /**
     * 搜索org.json.JSONObject/JSONArray
     */
    private String sniffJsonOrgObjects(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
            "org.json.JSONObject",
            "org.json.JSONArray"
        };

        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;

                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    fields.put("library", "org.json");
                    fields.put("nodeType", className.substring(className.lastIndexOf('.') + 1));

                    // 尝试获取JSON内容
                    try {
                        String jsonStr = heapHolder.toString(instance);
                        if (jsonStr != null && !jsonStr.isEmpty()) {
                            if (jsonStr.length() <= 500) {
                                fields.put("content", jsonStr);
                            } else {
                                fields.put("content", jsonStr.substring(0, 500) + "...");
                                fields.put("truncated", "true");
                            }
                        }
                    } catch (Exception ignored) {}

                    // Map (internal map)
                    try {
                        Object map = heapHolder.getFieldValue(instance, "map");
                        if (map != null) {
                            HashMap<String, String> mapContents = heapHolder.arrayDump(heapHolder.getMap(map));
                            if (mapContents != null) {
                                fields.put("size", String.valueOf(mapContents.size()));
                            }
                        }
                    } catch (Exception ignored) {}

                    result.append("[OrgJson] ").append(HashMapUtils.dumpString(fields, false)).append("\n");
                }
            } catch (Exception ignored) {}
        }

        return result.toString();
    }

    /**
     * 验证字符串是否为有效的JSON格式
     */
    private boolean isValidJson(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        String trimmed = str.trim();

        // 快速检查
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return false;
        }

        if (!trimmed.endsWith("}") && !trimmed.endsWith("]")) {
            return false;
        }

        // 简单的括号匹配检查
        try {
            int braces = 0;
            int brackets = 0;
            boolean inString = false;
            boolean escaped = false;

            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inString = !inString;
                    continue;
                }

                if (!inString) {
                    if (c == '{') braces++;
                    else if (c == '}') braces--;
                    else if (c == '[') brackets++;
                    else if (c == ']') brackets--;

                    // 如果括号数变为负数，说明格式错误
                    if (braces < 0 || brackets < 0) {
                        return false;
                    }
                }
            }

            // 最终括号数应该为0
            return braces == 0 && brackets == 0;
        } catch (Exception e) {
            return false;
        }
    }
}