package cn.wanghw;

/**
 * 敏感信息分类枚举
 * 用于对检测结果进行分类展示和管理
 */
public enum SensitivityCategory {
    /**
     * 凭证类（密码、密钥、Token等）
     */
    CREDENTIAL("🔑", "凭证", "#f38ba8", "#eba0ac"),

    /**
     * 个人信息（手机、邮箱、身份证等）
     */
    PII("📱", "个人信息", "#fab387", "#f5a97f"),

    /**
     * 会话数据（Cookie、Session等）
     */
    SESSION("🍪", "会话数据", "#f9e2af", "#eed49f"),

    /**
     * 网络信息（IP、URL等）
     */
    NETWORK("🌐", "网络信息", "#89dceb", "#74c7ec"),

    /**
     * 日志数据（日志中的敏感信息）
     */
    LOG("📝", "日志数据", "#a6adc8", "#9399b2"),

    /**
     * 数据库连接（数据库密码、连接字符串）
     */
    DATABASE("💾", "数据库", "#cba6f7", "#b4befe"),

    /**
     * 云服务密钥（AWS、Azure、GCP等）
     */
    CLOUD("☁️", "云服务", "#f5c2e7", "#eba0ac"),

    /**
     * 配置信息（配置文件中的敏感信息）
     */
    CONFIG("⚙️", "配置信息", "#94e2d5", "#7dc4e4");

    private final String icon;
    private final String name;
    private final String backgroundColor;
    private final String hoverColor;

    SensitivityCategory(String icon, String name, String backgroundColor, String hoverColor) {
        this.icon = icon;
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.hoverColor = hoverColor;
    }

    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getHoverColor() {
        return hoverColor;
    }

    /**
     * 获取默认严重级别
     */
    public Severity getDefaultSeverity() {
        switch (this) {
            case CREDENTIAL:
                return Severity.CRITICAL;
            case PII:
                return Severity.HIGH;
            case SESSION:
                return Severity.HIGH;
            case NETWORK:
                return Severity.MEDIUM;
            case LOG:
                return Severity.MEDIUM;
            case DATABASE:
                return Severity.HIGH;
            case CLOUD:
                return Severity.CRITICAL;
            case CONFIG:
                return Severity.MEDIUM;
            default:
                return Severity.INFO;
        }
    }

    /**
     * 从字符串映射到分类枚举（用于YAML解析）
     */
    public static SensitivityCategory fromString(String category) {
        if (category == null || category.isEmpty()) {
            return CONFIG;
        }

        switch (category.toLowerCase()) {
            case "credential":
            case "auth":
                return CREDENTIAL;
            case "pii":
                return PII;
            case "session":
                return SESSION;
            case "network":
                return NETWORK;
            case "log":
                return LOG;
            case "database":
                return DATABASE;
            case "cloud":
                return CLOUD;
            case "general":
            case "mq":
            case "framework":
                return CONFIG;
            default:
                return CONFIG;
        }
    }

    /**
     * 获取分类显示文本（图标 + 名称）
     */
    public String getDisplayText() {
        return icon + " " + name;
    }
}