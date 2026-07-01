package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract JWT tokens, OAuth2 tokens, Bearer tokens from heap memory.
 * Searches String pool for token patterns AND inspects known token holder classes.
 */
public class TokenSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenSearch.class);


    public String getName() { return "TokenSearch"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract JWT tokens, OAuth2 access tokens, Bearer tokens from heap"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    // JWT pattern: header.payload.signature (3 base64url segments separated by dots)
    private static final Pattern JWT_PATTERN = CredentialPatterns.JWT_TOKEN;

    // Bearer token pattern
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)bearer\\s+[A-Za-z0-9_\\-.~+/]+=*");

    // OAuth2 access_token pattern (in JSON)
    private static final Pattern OAUTH2_PATTERN = Pattern.compile(
            "\"access_token\"\\s*:\\s*\"([^\"]+)\"");

    // API Key patterns
    private static final Pattern APIKEY_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|apikey|access[_-]?key|secret[_-]?key)\\s*[=:]\\s*[A-Za-z0-9_\\-/+]{16,}");

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // 1. Search structured token holder classes
            sniffTokenClasses(heapHolder, result);

            // 2. Search String pool for JWT patterns
            sniffJwtStrings(heapHolder, result);

            // 3. Search String pool for Bearer tokens
            sniffBearerStrings(heapHolder, result);

        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }

    private void sniffTokenClasses(IHeapHolder heapHolder, StringBuilder result) {
        // OAuth2AccessToken
        Object clazz1 = heapHolder.findClass("org.springframework.security.oauth2.common.OAuth2AccessToken");
        if (clazz1 == null)
            clazz1 = heapHolder.findClass("org.springframework.security.oauth2.core.OAuth2AccessToken");
        if (clazz1 != null) {
            HashMap<String, String> fieldList1 = new HashMap<String, String>() {{
                put("tokenValue", "tokenValue");
                put("tokenType", "tokenType");
                put("scopes", "scopes");
            }};
            for (Object instance : heapHolder.getInstances(clazz1)) {
                result.append("[OAuth2AccessToken] ").append(
                        HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList1), false));
            }
        }

        // JwtAccessTokenConverter
        Object clazz2 = heapHolder.findClass("org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter");
        if (clazz2 != null) {
            for (Object instance : heapHolder.getInstances(clazz2)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[JwtAccessTokenConverter] ").append(HashMapUtils.dumpString(vals, false));
            }
        }

        // Spring Security OAuth2 AuthorizedClient
        Object clazz3 = heapHolder.findClass("org.springframework.security.oauth2.client.OAuth2AuthorizedClient");
        if (clazz3 != null) {
            for (Object instance : heapHolder.getInstances(clazz3)) {
                Object accessToken = heapHolder.getFieldValue(instance, "accessToken");
                if (accessToken != null) {
                    String tokenValue = heapHolder.getFieldStringValue(accessToken, "tokenValue");
                    result.append("[OAuth2AuthorizedClient] tokenValue=").append(tokenValue).append("\n");
                }
            }
        }

        // Nimbus JWT
        Object clazz4 = heapHolder.findClass("com.nimbusds.oauth2.sdk.token.BearerAccessToken");
        if (clazz4 != null) {
            for (Object instance : heapHolder.getInstances(clazz4)) {
                String value = heapHolder.getFieldStringValue(instance, "value");
                result.append("[BearerAccessToken] value=").append(value).append("\n");
            }
        }

        // JwtDecoders (Spring Security)
        Object clazz5 = heapHolder.findClass("org.springframework.security.oauth2.jwt.NimbusJwtDecoder");
        if (clazz5 != null) {
            for (Object instance : heapHolder.getInstances(clazz5)) {
                String jwkSetUri = heapHolder.getFieldStringValue(instance, "jwkSetUri");
                String issuer = heapHolder.getFieldStringValue(instance, "issuer");
                result.append("[NimbusJwtDecoder] jwkSetUri=").append(jwkSetUri)
                        .append(", issuer=").append(issuer).append("\n");
            }
        }

        // Pac4j Token
        Object clazz6 = heapHolder.findClass("org.pac4j.core.profile.jwt.JwtProfile");
        if (clazz6 != null) {
            for (Object instance : heapHolder.getInstances(clazz6)) {
                result.append("[JwtProfile] ").append(
                        HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
            }
        }
    }

    private void sniffJwtStrings(IHeapHolder heapHolder, StringBuilder result) {
        try {
            List<String> matches = heapHolder.searchStrings(JWT_PATTERN);
            if (!matches.isEmpty()) {
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (String jwt : matches) {
                    if (seen.add(jwt)) {
                        result.append("[JWTString] ").append(jwt).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void sniffBearerStrings(IHeapHolder heapHolder, StringBuilder result) {
        try {
            List<String> matches = heapHolder.searchStrings(BEARER_PATTERN);
            if (!matches.isEmpty()) {
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (String bearer : matches) {
                    if (seen.add(bearer)) {
                        result.append("[BearerToken] ").append(bearer).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
