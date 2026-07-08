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
 * Extract API keys, secrets, access keys, and various auth credentials from heap.
 * Covers: AWS, Aliyun, Tencent Cloud, Huawei Cloud, Stripe, SendGrid, etc.
 */
public class AuthTokenSearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenSearch.class);


    public String getName() { return "AuthTokenSearch"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract API keys, access keys, secrets from cloud providers and services"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.CRITICAL; }

    // Patterns for API keys in string pool
    private static final Pattern[] KEY_PATTERNS = {
            // AWS Access Key ID
            CredentialPatterns.AWS_ACCESS_KEY_ID,
            // AWS Secret Access Key (40 chars base64-ish)
            CredentialPatterns.AWS_SECRET_ACCESS_KEY,
            // Aliyun AccessKey ID (LTAI prefix)
            CredentialPatterns.ALIYUN_ACCESS_KEY_ID,
            // Tencent Cloud SecretId
            CredentialPatterns.TENCENT_SECRET_ID,
            // Stripe API Key
            Pattern.compile("(?:sk|pk)_(?:test|live)_[A-Za-z0-9]{24,}"),
            // SendGrid API Key
            Pattern.compile("SG\\.[A-Za-z0-9_\\-]{22}\\.[A-Za-z0-9_\\-]{43}"),
            // GitHub Token
            Pattern.compile("gh[pousr]_[A-Za-z0-9_]{36,}"),
            // Slack Token
            Pattern.compile("xox[baprs]-[A-Za-z0-9\\-]{10,}"),
            // Telegram Bot Token
            Pattern.compile("[0-9]{8,10}:[A-Za-z0-9_\\-]{35}"),
            // Private Key header
            Pattern.compile("-----BEGIN (?:RSA |EC |DSA )?PRIVATE KEY-----"),
            // Generic token patterns in strings
            Pattern.compile("(?i)(?:secret[_-]?key|access[_-]?key|api[_-]?key|auth[_-]?token|private[_-]?key)\\s*[=:]\\s*[A-Za-z0-9_\\-/+=]{16,}")
    };

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // 1. Search structured cloud SDK classes
            sniffCloudSDKClasses(heapHolder, result);
            // 2. Search Spring Security related
            sniffSecurityClasses(heapHolder, result);
            // 3. Search String pool for key patterns
            sniffKeyPatterns(heapHolder, result);
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }

    private void sniffCloudSDKClasses(IHeapHolder heapHolder, StringBuilder result) {
        // AWS credentials
        Object awsCreds = heapHolder.findClass("com.amazonaws.auth.BasicAWSCredentials");
        if (awsCreds == null)
            awsCreds = heapHolder.findClass("software.amazon.awssdk.auth.credentials.AwsBasicCredentials");
        if (awsCreds != null) {
            HashMap<String, String> fieldList = new HashMap<String, String>() {{
                put("accessKey", "accessKey");
                put("secretKey", "secretKey");
                put("awsAccessKeyId", "awsAccessKeyId");
                put("awsSecretKey", "awsSecretKey");
            }};
            for (Object instance : heapHolder.getInstances(awsCreds)) {
                result.append("[AWSCredentials] ").append(
                        HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
            }
        }

        // Aliyun credentials
        Object aliCreds = heapHolder.findClass("com.aliyuncs.auth.AlibabaCloudCredentials");
        if (aliCreds == null)
            aliCreds = heapHolder.findClass("com.aliyuncs.auth.BasicCredentials");
        if (aliCreds != null) {
            for (Object instance : heapHolder.getInstances(aliCreds)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[AliyunCredentials] ").append(HashMapUtils.dumpString(vals, false));
            }
        }

        // Aliyun DefaultProfile
        Object aliProfile = heapHolder.findClass("com.aliyuncs.DefaultProfile");
        if (aliProfile != null) {
            for (Object instance : heapHolder.getInstances(aliProfile)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[AliyunProfile] ").append(HashMapUtils.dumpString(vals, false));
            }
        }

        // Tencent Cloud credentials
        Object tencentCreds = heapHolder.findClass("com.tencent.cloud.cos.credentials.BasicCOSCredentials");
        if (tencentCreds != null) {
            for (Object instance : heapHolder.getInstances(tencentCreds)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[TencentCOSCredentials] ").append(HashMapUtils.dumpString(vals, false));
            }
        }

        // Huawei Cloud credentials
        Object hwCreds = heapHolder.findClass("com.huaweicloud.sdk.core.auth.BasicCredentials");
        if (hwCreds == null)
            hwCreds = heapHolder.findClass("com.huaweicloud.sdk.core.auth.GlobalCredentials");
        if (hwCreds != null) {
            for (Object instance : heapHolder.getInstances(hwCreds)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[HuaweiCredentials] ").append(HashMapUtils.dumpString(vals, false));
            }
        }

        // Minio credentials
        Object minioCreds = heapHolder.findClass("io.minio.credentials.Credentials");
        if (minioCreds != null) {
            for (Object instance : heapHolder.getInstances(minioCreds)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[MinioCredentials] ").append(HashMapUtils.dumpString(vals, false));
            }
        }
    }

    private void sniffSecurityClasses(IHeapHolder heapHolder, StringBuilder result) {
        // Spring Security UserDetailsService stored credentials
        Object userDetails = heapHolder.findClass("org.springframework.security.core.userdetails.User");
        if (userDetails != null) {
            for (Object instance : heapHolder.getInstances(userDetails)) {
                String username = heapHolder.getFieldStringValue(instance, "username");
                String password = heapHolder.getFieldStringValue(instance, "password");
                if (password != null && !password.isEmpty()) {
                    result.append("[SpringSecurityUser] username=").append(username)
                            .append(", password=").append(password).append("\n");
                }
            }
        }

        // Spring Security RememberMe token
        Object rememberMe = heapHolder.findClass("org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken");
        if (rememberMe != null) {
            for (Object instance : heapHolder.getInstances(rememberMe)) {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                result.append("[RememberMeToken] ").append(HashMapUtils.dumpString(vals, false));
            }
        }

        // Pac4j credentials/profiles
        Object pac4jProfile = heapHolder.findClass("org.pac4j.core.profile.CommonProfile");
        if (pac4jProfile != null) {
            for (Object instance : heapHolder.getInstances(pac4jProfile)) {
                String id = heapHolder.getFieldStringValue(instance, "id");
                result.append("[Pac4jProfile] id=").append(id).append("\n");
            }
        }

        // Spring Boot Admin client credentials (already covered by UserPassSearcher, but more targeted)
        Object sbaClient = heapHolder.findClass("de.codecentric.boot.admin.client.config.ClientProperties");
        if (sbaClient != null) {
            HashMap<String, String> fieldList = new HashMap<String, String>() {{
                put("url", "url");
                put("username", "username");
                put("password", "password");
            }};
            for (Object instance : heapHolder.getInstances(sbaClient)) {
                result.append("[SpringBootAdmin] ").append(
                        HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
            }
        }
    }

    private void sniffKeyPatterns(IHeapHolder heapHolder, StringBuilder result) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Pattern pattern : KEY_PATTERNS) {
            try {
                List<String> matches = heapHolder.searchAll(pattern);
                for (String match : matches) {
                    if (seen.add(match)) {
                        result.append("[KeyPattern] ").append(match).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
