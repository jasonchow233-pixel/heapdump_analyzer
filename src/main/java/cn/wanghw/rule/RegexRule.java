package cn.wanghw.rule;

import cn.wanghw.IHeapHolder;
import cn.wanghw.Severity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class RegexRule implements Rule {
    private String id;
    private String name;
    private String category;
    private Severity severity;
    private String description;
    private Pattern pattern;
    private Validator validator;
    private boolean useRawMemory = false;  // 是否使用原始内存扫描

    public RegexRule() {}

    public RegexRule(String id, String name, String category, Severity severity,
                     String description, Pattern pattern) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.severity = severity;
        this.description = description;
        this.pattern = pattern;
    }

    @Override
    public RuleResult execute(IHeapHolder heapHolder) {
        try {
            List<String> matches = heapHolder.searchAll(pattern);
            Set<String> unique = new HashSet<>(matches);
            return new RuleResult(this, List.copyOf(unique));
        } catch (Exception e) {
            return RuleResult.empty(this);
        }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Pattern getPattern() { return pattern; }
    public void setPattern(Pattern pattern) { this.pattern = pattern; }
    @Override
    public Validator getValidator() { return validator; }
    public void setValidator(Validator validator) { this.validator = validator; }
    public boolean isUseRawMemory() { return useRawMemory; }
    public void setUseRawMemory(boolean useRawMemory) { this.useRawMemory = useRawMemory; }
}
