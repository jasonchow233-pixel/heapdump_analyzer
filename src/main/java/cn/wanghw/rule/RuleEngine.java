package cn.wanghw.rule;

import cn.wanghw.IHeapHolder;
import cn.wanghw.SensitivityCategory;
import cn.wanghw.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RuleEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleEngine.class);

    private final List<Rule> rules;
    private boolean validateEnabled = false;
    private boolean validateLiveEnabled = false;
    private Severity minSeverity = Severity.INFO;
    private boolean parallelEnabled = false;
    private int threadCount = 0;
    private boolean useRawMemory = false;  // 是否使用原始内存扫描
    private String categoryFilter = "";     // 分类过滤

    public RuleEngine(List<Rule> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }

    public void setValidateEnabled(boolean enabled) {
        this.validateEnabled = enabled;
    }

    public void setValidateLiveEnabled(boolean enabled) {
        this.validateLiveEnabled = enabled;
        if (enabled) {
            this.validateEnabled = true;
        }
    }

    public void setMinSeverity(Severity severity) {
        this.minSeverity = severity;
    }

    public void setParallelEnabled(boolean enabled) {
        this.parallelEnabled = enabled;
    }

    public void setThreadCount(int count) {
        this.threadCount = count;
    }

    public void setUseRawMemory(boolean useRawMemory) {
        this.useRawMemory = useRawMemory;
        // 为所有RegexRule设置原始内存扫描选项
        for (Rule rule : rules) {
            if (rule instanceof RegexRule) {
                ((RegexRule) rule).setUseRawMemory(useRawMemory);
            }
        }
    }

    public void setCategoryFilter(String category) {
        this.categoryFilter = category != null ? category : "";
    }

    public List<RuleResult> execute(IHeapHolder heapHolder) {
        List<Rule> eligible = new ArrayList<>();
        SensitivityCategory targetCategory = null;

        // 如果设置了分类过滤，先解析目标分类
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            targetCategory = SensitivityCategory.fromString(categoryFilter);
        }

        for (Rule rule : rules) {
            // 严重级别过滤
            if (rule.getSeverity().ordinal() > minSeverity.ordinal()) {
                continue;
            }
            // 分类过滤
            if (targetCategory != null) {
                SensitivityCategory ruleCategory = SensitivityCategory.fromString(rule.getCategory());
                if (ruleCategory != targetCategory) {
                    continue;
                }
            }
            eligible.add(rule);
        }

        if (eligible.isEmpty()) return new ArrayList<>();

        if (parallelEnabled && eligible.size() > 1) {
            return executeParallel(heapHolder, eligible);
        }
        return executeSequential(heapHolder, eligible);
    }

    private List<RuleResult> executeSequential(IHeapHolder heapHolder, List<Rule> eligible) {
        List<RuleResult> results = new ArrayList<>();
        for (Rule rule : eligible) {
            RuleResult result = runOne(rule, heapHolder);
            if (result != null) results.add(result);
        }
        return results;
    }

    private List<RuleResult> executeParallel(IHeapHolder heapHolder, List<Rule> eligible) {
        int threads = threadCount > 0 ? threadCount : Math.min(Runtime.getRuntime().availableProcessors(), 8);
        threads = Math.min(threads, eligible.size());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<RuleResult>> tasks = new ArrayList<>(eligible.size());
            for (Rule rule : eligible) {
                tasks.add(() -> runOne(rule, heapHolder));
            }
            List<RuleResult> results = new ArrayList<>();
            List<Future<RuleResult>> futures = executor.invokeAll(tasks);
            for (Future<RuleResult> f : futures) {
                try {
                    RuleResult rr = f.get();
                    if (rr != null) results.add(rr);
                } catch (Exception e) {
                    LOGGER.warn("Rule task failed: {}", e.getMessage());
                }
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Rule engine parallel execution interrupted");
            return new ArrayList<>();
        } finally {
            executor.shutdown();
        }
    }

    private RuleResult runOne(Rule rule, IHeapHolder heapHolder) {
        try {
            RuleResult result = rule.execute(heapHolder);
            if (!result.isFound()) return null;
            if (validateEnabled) {
                Validator v = rule.getValidator();
                if (v != null && !result.getMatches().isEmpty()) {
                    List<String> validated = v.validate(result.getMatches());
                    result = result.withValidated(validated);
                    if (validateLiveEnabled && v instanceof LiveValidator) {
                        List<CredentialCheckResult> live = ((LiveValidator) v).validateLive(result.getMatches());
                        result = result.withLiveResults(live);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.warn("Rule execution error [{}]: {}", rule.getName(), e.getMessage());
            return null;
        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    public List<Rule> getRulesByCategory(String category) {
        return rules.stream()
                .filter(r -> category.equals(r.getCategory()))
                .collect(Collectors.toList());
    }

    public int getRuleCount() {
        return rules.size();
    }

    /**
     * 执行所有规则并返回EnhancedResult列表
     * 每个匹配项都会创建一个EnhancedResult对象
     */
    public List<EnhancedResult> executeEnhanced(IHeapHolder heapHolder) {
        List<EnhancedResult> enhancedResults = new ArrayList<>();
        List<RuleResult> ruleResults = execute(heapHolder);

        for (RuleResult ruleResult : ruleResults) {
            if (ruleResult != null && ruleResult.isFound()) {
                // 从规则的category字段映射到SensitivityCategory
                SensitivityCategory category = SensitivityCategory.fromString(ruleResult.getRule().getCategory());
                // 每个匹配项创建一个EnhancedResult
                List<EnhancedResult> results = EnhancedResult.fromRuleResult(ruleResult, category);
                enhancedResults.addAll(results);
            }
        }

        return enhancedResults;
    }
}
