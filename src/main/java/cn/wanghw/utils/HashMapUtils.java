package cn.wanghw.utils;

import java.util.HashMap;
import java.util.TreeMap;

public class HashMapUtils {
    public static String dumpString(HashMap<String, String> hashMap) {
        return dumpString(hashMap, true);
    }

    public static String dumpString(HashMap<String, String> hashMap, boolean oneline) {
        return dumpString(hashMap, oneline, true, false);
    }

    public static String dumpString(HashMap<String, String> hashMap, boolean oneline, boolean newLine, boolean ignoreNull) {
        return dumpStringAligned(hashMap, oneline, newLine, ignoreNull, false);
    }

    /**
     * 格式化输出 HashMap，支持自动对齐
     * @param hashMap 要格式化的 HashMap
     * @param oneline 是否在一行显示（true = 用逗号分隔，false = 每行一个条目）
     * @param newLine 是否在末尾添加换行
     * @param ignoreNull 是否忽略 null 或空值
     * @param align 是否自动对齐 key（仅在 multiline 模式下有效）
     * @return 格式化后的字符串
     */
    public static String dumpStringAligned(HashMap<String, String> hashMap, boolean oneline, boolean newLine, boolean ignoreNull, boolean align) {
        // 使用 TreeMap 按 key 排序，使输出更有条理
        TreeMap<String, String> sortedMap = new TreeMap<>(hashMap);

        Object[] allKey = sortedMap.keySet().toArray();

        // 计算最长 key 的长度（用于对齐）
        int maxKeyLength = 0;
        if (align && !oneline) {
            for (Object keyObj : allKey) {
                String key = keyObj.toString();
                if (!ignoreNull || (sortedMap.get(key) != null && !sortedMap.get(key).equals(""))) {
                    maxKeyLength = Math.max(maxKeyLength, key.length());
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < allKey.length; i++) {
            String key = allKey[i].toString();
            String value = sortedMap.get(key);

            if (ignoreNull && (value == null || value.equals(""))) continue;

            // 对齐格式：填充空格使所有 key 右对齐到相同长度
            if (align && !oneline && maxKeyLength > 0) {
                String paddedKey = String.format("%-" + maxKeyLength + "s", key);
                result.append(paddedKey).append(" = ").append(value).append("\n");
            } else {
                result.append(key).append(" = ").append(value);
                if (oneline) {
                    if (i + 1 < allKey.length) {
                        result.append(", ");
                    }
                } else {
                    result.append("\n");
                }
            }
        }

        String output = result.toString();
        // 移除末尾多余的逗号和空格（单行模式）
        if (output.endsWith(", ")) {
            output = output.substring(0, output.length() - 2);
        }
        // 移除末尾多余的换行（多行模式）
        if (!oneline && output.endsWith("\n")) {
            output = output.substring(0, output.length() - 1);
        }

        // 单行模式下添加末尾换行（如果需要）
        return !output.isEmpty() && oneline && newLine ? output + "\n" : output;
    }

}
