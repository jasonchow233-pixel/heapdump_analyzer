package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Extract ClassLoader information to identify loaded libraries and their versions.
 * java.net.URLClassLoader
 */
public class ClassLoaderSearch implements ISpider {
    @Override
    public String getName() {
        return "ClassLoader";
    }

    @Override
    public String getCategory() {
        return "security";
    }

    @Override
    public String getDescription() {
        return "Extract ClassLoader URLs to identify loaded JAR files and libraries";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("java.net.URLClassLoader");
            if (clazz == null) return "";
            for (Object instance : heapHolder.getInstances(clazz)) {
                HashMap<String, String> fields = new HashMap<>();
                try {
                    Object ucp = heapHolder.getFieldValue(instance, "ucp");
                    if (ucp != null) {
                        Object pathList = heapHolder.getFieldValue(ucp, "pathList");
                        if (pathList != null) {
                            // pathList is an ArrayList of URLClassLoader.PathVar
                            String pathStr = heapHolder.toString(pathList);
                            if (pathStr != null && !pathStr.isEmpty()) {
                                // Filter for interesting JARs
                                String[] lines = pathStr.split("\n");
                                StringBuilder interesting = new StringBuilder();
                                for (String line : lines) {
                                    if (line.contains(".jar") &&
                                        (line.contains("spring") || line.contains("log4j") ||
                                         line.contains("jackson") || line.contains("tomcat") ||
                                         line.contains("netty") || line.contains("shiro") ||
                                         line.contains("security") || line.contains("auth") ||
                                         line.contains("crypto") || line.contains("kafka") ||
                                         line.contains("redis") || line.contains("mongo") ||
                                         line.contains("mysql") || line.contains("postgres"))) {
                                        interesting.append(line).append("\n");
                                    }
                                }
                                if (interesting.length() > 0) {
                                    fields.put("interestingJars", interesting.toString().trim());
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                if (!fields.isEmpty()) {
                    result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                }
            }
        } catch (Exception ignored) {}
        return result.toString();
    }
}
