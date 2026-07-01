package cn.wanghw.spider;

import java.util.regex.Pattern;

/**
 * Shared regex patterns for credential detection in the heap string pool.
 * Centralises patterns that are referenced by both specialised cloud-credential
 * Spiders (e.g. {@link AWSCredentialSearch}) and the generalist
 * {@link AuthTokenSearch}, preventing the same regex from drifting apart
 * across files.
 *
 * <p>YAML rule files under {@code src/main/resources/rules/} intentionally
 * keep their own copies because SnakeYAML cannot reference Java constants;
 * the patterns here are the canonical source for Java code.</p>
 */
public final class CredentialPatterns {

    private CredentialPatterns() {}

    public static final Pattern AWS_ACCESS_KEY_ID =
            Pattern.compile("AKIA[0-9A-Z]{16}");

    public static final Pattern AWS_SECRET_ACCESS_KEY =
            Pattern.compile("(?i)aws[_-]?secret[_-]?access[_-]?key\\s*[=:]\\s*[A-Za-z0-9/+=]{40}");

    public static final Pattern ALIYUN_ACCESS_KEY_ID =
            Pattern.compile("LTAI[A-Za-z0-9]{12,20}");

    public static final Pattern TENCENT_SECRET_ID =
            Pattern.compile("AKID[A-Za-z0-9]{32}");

    public static final Pattern JWT_TOKEN =
            Pattern.compile("eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
}
