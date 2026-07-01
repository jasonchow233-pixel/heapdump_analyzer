package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiroKeyEnhanced implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiroKeyEnhanced.class);


    public String getName() {
        return "ShiroKeyEnhanced";
    }

    public String getCategory() {
        return "crypto";
    }

    public String getDescription() {
        return "Extract Apache Shiro keys from multiple configuration sources";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.CRITICAL;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // IniSecurityManager
            Object clazz1 = heapHolder.findClass("org.apache.shiro.web.config.IniFilterChainResolverFactory");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    Object ini = heapHolder.getFieldValue(instance, "ini");
                    if (ini != null) {
                        result.append("[IniFilterConfig] ").append(
                                HashMapUtils.dumpString(heapHolder.getAllFieldValues(ini), false));
                    }
                }
            }
            // Shiro CookieRememberMeManager cipherKey
            Object clazz2 = heapHolder.findClass("org.apache.shiro.web.mgt.CookieRememberMeManager");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("org.apache.shiro.mgt.AbstractRememberMeManager");
            if (clazz2 != null) {
                HashMap<String, String> fieldList2 = new HashMap<String, String>() {{
                    put("cipherService.algorithmName", "algorithmName");
                    put("cipherKey", "cipherKey");
                    put("encryptionCipherKey", "encryptionCipherKey");
                    put("decryptionCipherKey", "decryptionCipherKey");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> values = heapHolder.getFieldsByNameList(instance, fieldList2);
                    String alg = heapHolder.getFieldStringValue(instance, "cipherService.algorithmName");
                    if (alg != null) values.put("algorithmName", alg);
                    result.append("[RememberMeManager] ").append(HashMapUtils.dumpString(values, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
