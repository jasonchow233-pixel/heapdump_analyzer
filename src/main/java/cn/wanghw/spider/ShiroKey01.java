package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.Base64;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiroKey01 implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiroKey01.class);


    public String getName() {
        return "CookieRememberMeManager(ShiroKey)";
    }


    public String sniff(IHeapHolder heapHolder) {

        final StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("org.apache.shiro.web.mgt.CookieRememberMeManager");
            if (clazz == null)
                return null;
            for (Object instance : heapHolder.getInstances(clazz)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("algName", heapHolder.getFieldStringValue(instance, "cipherService.algorithmName"));
                values.put("algMode", heapHolder.getFieldStringValue(instance, "cipherService.modeName"));
                Object encryptionCipherKey = heapHolder.getFieldValue(instance, "encryptionCipherKey");
                if (encryptionCipherKey != null) {
                    byte[] key = heapHolder.toByteArray(encryptionCipherKey);
                    if (key != null) {
                        values.put("key", Base64.encode(key));
                    }
                }
                result.append(HashMapUtils.dumpString(values));
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.toString();
    }
}