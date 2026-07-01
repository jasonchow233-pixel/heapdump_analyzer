package cn.wanghw.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleResult {
    private final Rule rule;
    private final List<String> matches;
    private final List<String> validated;
    private final List<CredentialCheckResult> liveResults;
    private final boolean found;

    public RuleResult(Rule rule, List<String> matches) {
        this(rule, matches, Collections.emptyList(), Collections.emptyList());
    }

    public RuleResult(Rule rule, List<String> matches, List<String> validated) {
        this(rule, matches, validated, Collections.emptyList());
    }

    public RuleResult(Rule rule, List<String> matches, List<String> validated, List<CredentialCheckResult> liveResults) {
        this.rule = rule;
        this.matches = matches != null ? matches : new ArrayList<>();
        this.validated = validated != null ? validated : new ArrayList<>();
        this.liveResults = liveResults != null ? liveResults : new ArrayList<>();
        this.found = !this.matches.isEmpty();
    }

    public static RuleResult empty(Rule rule) {
        return new RuleResult(rule, Collections.emptyList());
    }

    public RuleResult withValidated(List<String> validated) {
        return new RuleResult(rule, matches, validated, liveResults);
    }

    public RuleResult withLiveResults(List<CredentialCheckResult> liveResults) {
        return new RuleResult(rule, matches, validated, liveResults);
    }

    public Rule getRule() { return rule; }
    public List<String> getMatches() { return matches; }
    public List<String> getValidated() { return validated; }
    public List<CredentialCheckResult> getLiveResults() { return liveResults; }
    public boolean isFound() { return found; }

    public boolean hasLiveResults() {
        return liveResults != null && !liveResults.isEmpty();
    }

    @Override
    public String toString() {
        if (!found) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(rule.getName()).append("] ").append(rule.getDescription()).append("\n");
        for (String match : matches) {
            sb.append("  ").append(match).append("\n");
        }
        if (!validated.isEmpty()) {
            sb.append("  ── Offline Validation ──\n");
            for (String v : validated) {
                sb.append("  ").append(v).append("\n");
            }
        }
        if (hasLiveResults()) {
            sb.append("  ── Live Validation ──\n");
            for (CredentialCheckResult c : liveResults) {
                sb.append("  ").append(c).append("\n");
            }
        }
        return sb.toString();
    }
}
