package cn.wanghw;

import java.util.regex.Pattern;

/**
 * 敏感信息类型枚举，用于UI快速搜索过滤
 * 预定义常见敏感信息的正则模式，方便用户快速过滤
 */
public enum SensitivityType {
    ALL("All Types", null, null),

    JWT("JWT Token",
        "JSON Web Tokens (eyJ header)",
        Pattern.compile("eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")),

    BEARER_TOKEN("Bearer Token",
        "Bearer authentication tokens",
        Pattern.compile("(?i)bearer\\s+[A-Za-z0-9_\\-.~+/]+=*")),

    OAUTH2_TOKEN("OAuth2 Token",
        "OAuth2 access tokens in JSON",
        Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"")),

    PASSWORD("Password Field",
        "Password fields with values",
        Pattern.compile("(?i)(password|passwd|pwd|密码|口令|密钥)\\s*[=:]\\s*[^\\s<>\"]{6,}")),

    USERNAME_PASSWORD("Username+Password",
        "Username and password pairs",
        Pattern.compile("(?i)(username|user|login|账号|用户名)[\\s]*[=:][\\s]*['\"]?([^\\s<>\"']{3,})['\"]?[\\s,\\n]*(password|passwd|pwd|密码|口令)[\\s]*[=:][\\s]*['\"]?([^\\s<>\"']{6,})['\"]?")),

    API_KEY("API Key",
        "API keys and secrets",
        Pattern.compile("(?i)(?:secret[_-]?key|access[_-]?key|api[_-]?key|auth[_-]?token|private[_-]?key)\\s*[=:]\\s*[A-Za-z0-9_\\-/+=]{16,}")),

    COOKIE("Cookie/Header",
        "HTTP cookies and authorization headers",
        Pattern.compile("(?i)(cookie|authorization|set-cookie)\\s*[=:]")),

    PRIVATE_KEY("Private Key",
        "Private key files (RSA, DSA, EC)",
        Pattern.compile("-----BEGIN (?:RSA |DSA |EC |OPENSSH )?PRIVATE KEY-----[\\s\\S]*?-----END (?:RSA |DSA |EC |OPENSSH )?PRIVATE KEY-----")),

    DATABASE_CONN("Database Connection",
        "Database connection strings",
        Pattern.compile("(?i)(jdbc:[a-z]+://|mysql://|postgresql://|oracle:)[^\\s<>\"]+")),

    CLOUD_KEY("Cloud Credential",
        "Cloud provider credentials (AWS, Aliyun, etc.)",
        Pattern.compile("(AKIA[0-9A-Z]{16}|LTAI[A-Za-z0-9]{12,20}|AKID[A-Za-z0-9]{32})"));

    private final String displayName;
    private final String description;
    private final Pattern pattern;

    SensitivityType(String displayName, String description, Pattern pattern) {
        this.displayName = displayName;
        this.description = description;
        this.pattern = pattern;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * 检查文本是否匹配该类型
     *
     * @param text 待检查的文本
     * @return 是否匹配
     */
    public boolean matches(String text) {
        if (pattern == null) return true;
        return pattern.matcher(text).find();
    }
}