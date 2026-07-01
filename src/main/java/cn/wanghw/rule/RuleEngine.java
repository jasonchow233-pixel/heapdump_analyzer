package cn.wanghw.rule;

import cn.wanghw.IHeapHolder;
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

    public List<RuleResult> execute(IHeapHolder heapHolder) {
        List<Rule> eligible = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.getSeverity().ordinal() <= minSeverity.ordinal()) {
                eligible.add(rule);
            }
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
}
