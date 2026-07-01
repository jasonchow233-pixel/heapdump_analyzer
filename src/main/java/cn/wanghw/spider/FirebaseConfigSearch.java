package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Firebase configuration and credentials from Google Firebase SDK
 * com.google.firebase.FirebaseApp, FirebaseOptions
 */
public class FirebaseConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "FirebaseConfig";
    }

    @Override
    public String getCategory() {
        return "cloud";
    }

    @Override
    public String getDescription() {
        return "Extract Google Firebase configuration (project ID, database URL, storage bucket, API key) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        // FirebaseApp instances
        try {
            Object clazz = heapHolder.findClass("com.google.firebase.FirebaseApp");
            if (clazz != null) {
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String name = heapHolder.getFieldStringValue(instance, "name");
                    if (name != null) fields.put("appName", name);
                    Object options = heapHolder.getFieldValue(instance, "options");
                    if (options != null) {
                        extractFirebaseOptions(heapHolder, options, fields);
                    }
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {}

        // FirebaseOptions directly
        try {
            Object clazz = heapHolder.findClass("com.google.firebase.FirebaseOptions");
            if (clazz != null) {
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    extractFirebaseOptions(heapHolder, instance, fields);
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {}
        return result.toString();
    }

    private void extractFirebaseOptions(IHeapHolder heapHolder, Object options, HashMap<String, String> fields) {
        String[] fieldNames = {"projectId", "databaseUrl", "storageBucket",
                "messagingSenderId", "apiKey", "applicationId"};
        for (String fn : fieldNames) {
            try {
                String val = heapHolder.getFieldStringValue(options, fn);
                if (val != null && !val.isEmpty()) {
                    fields.put(fn, val);
                }
            } catch (Exception ignored) {}
        }
        // Try credential
        try {
            Object credential = heapHolder.getFieldValue(options, "credential");
            if (credential != null) {
                String email = heapHolder.getFieldStringValue(credential, "clientEmail");
                if (email != null) fields.put("credentialEmail", email);
            }
        } catch (Exception ignored) {}
    }
}
