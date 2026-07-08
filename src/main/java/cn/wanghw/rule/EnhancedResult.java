package cn.wanghw.rule;

import cn.wanghw.SensitivityCategory;
import cn.wanghw.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强的检测结果模型
 * 包含完整的敏感信息及相关上下文
 */
public class EnhancedResult {
    private final SensitivityCategory category;
    private final String type;
    private final String fullValue;        // 完整值（不脱敏）
    private final String location;         // 对象位置
    private final String context;          // 上下文信息
    private final Severity severity;
    private final String ruleId;           // 规则ID
    private final String pattern;          // 匹配的正则表达式

    public EnhancedResult(SensitivityCategory category, String type, String fullValue,
                          String location, String context, Severity severity,
                          String ruleId, String pattern) {
        this.category = category;
        this.type = type;
        this.fullValue = fullValue;
        this.location = location;
        this.context = context;
        this.severity = severity;
        this.ruleId = ruleId;
        this.pattern = pattern;
    }

    /**
     * 从RuleResult创建EnhancedResult列表（每个匹配项创建一个EnhancedResult）
     */
    public static List<EnhancedResult> fromRuleResult(RuleResult ruleResult, SensitivityCategory category) {
        List<EnhancedResult> results = new ArrayList<>();
        Rule rule = ruleResult.getRule();

        for (String match : ruleResult.getMatches()) {
            EnhancedResult result = new EnhancedResult(
                category,
                rule.getName(),
                match,                               // 完整值，不脱敏
                "Unknown",                           // 位置信息（待后续实现）
                "Found in heap dump",                // 上下文信息（待后续实现）
                rule.getSeverity(),
                rule.getId(),
                rule instanceof RegexRule ? ((RegexRule) rule).getPattern().pattern() : ""
            );
            results.add(result);
        }

        return results;
    }

    /**
     * 获取显示值（前50字符，超出显示省略）
     */
    public String getDisplayValue() {
        if (fullValue == null) {
            return "";
        }
        if (fullValue.length() > 50) {
            return fullValue.substring(0, 50) + "...[详情]";
        }
        return fullValue;
    }

    /**
     * 获取分类显示文本（图标 + 名称）
     */
    public String getCategoryDisplay() {
        return category.getDisplayText();
    }

    // Getters
    public SensitivityCategory getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getFullValue() {
        return fullValue;  // 完整返回，不做任何处理
    }

    public String getLocation() {
        return location;
    }

    public String getContext() {
        return context;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", severity, type, getDisplayValue());
    }
}