package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtKeyEnhanced implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtKeyEnhanced.class);


    public String getName() {
        return "JwtKeyEnhanced";
    }

    public String getCategory() {
        return "crypto";
    }

    public String getDescription() {
        return "Extract JWT signing keys from multiple JWT libraries (jjwt, nimbus-jose, auth0)";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.CRITICAL;
    }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // io.jsonwebtoken.impl.DefaultJwtBuilder
            Object clazz1 = heapHolder.findClass("io.jsonwebtoken.impl.DefaultJwtBuilder");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("key", "key");
                    put("secretKey", "secretKey");
                    put("keyBytes", "keyBytes");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> values = heapHolder.getFieldsByNameList(instance, fieldList);
                    if (values != null && !values.isEmpty())
                        result.append("[DefaultJwtBuilder] ").append(HashMapUtils.dumpString(values, false));
                }
            }
            // com.nimbusds.jwt.SignedJWT
            Object clazz2 = heapHolder.findClass("com.nimbusds.jwt.SignedJWT");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> values = heapHolder.getAllFieldValues(instance);
                    result.append("[SignedJWT] ").append(HashMapUtils.dumpString(values, false));
                }
            }
            // auth0 JWT
            Object clazz3 = heapHolder.findClass("com.auth0.jwt.JWT");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> values = heapHolder.getAllFieldValues(instance);
                    result.append("[Auth0JWT] ").append(HashMapUtils.dumpString(values, false));
                }
            }
            // Spring Security JWT decoder properties
            Object clazz4 = heapHolder.findClass("org.springframework.security.oauth2.jwt.JwtDecoderProperties");
            if (clazz4 != null) {
                HashMap<String, String> fieldList4 = new HashMap<String, String>() {{
                    put("jwkSetUri", "jwkSetUri");
                    put("issuerUri", "issuerUri");
                    put("restTemplate", "restTemplate");
                }};
                for (Object instance : heapHolder.getInstances(clazz4)) {
                    result.append("[JwtDecoderProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList4), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
