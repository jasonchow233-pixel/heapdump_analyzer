package cn.wanghw.rule;

/**
 * Outcome of validating a single credential candidate against a provider API.
 *
 * <p>Immutable. The {@code candidate} is the raw string found in the heap (possibly
 * truncated for display); {@code status} is the live verdict; {@code detail} carries
 * provider-specific context such as the resolved account/team name or an error message.
 */
public final class CredentialCheckResult {

    private final String candidate;
    private final CredentialStatus status;
    private final String detail;

    public CredentialCheckResult(String candidate, CredentialStatus status, String detail) {
        this.candidate = candidate;
        this.status = status;
        this.detail = detail != null ? detail : "";
    }

    public String getCandidate() {
        return candidate;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        String display = candidate.length() > 60 ? candidate.substring(0, 60) + "…" : candidate;
        if (detail.isEmpty()) {
            return display + " [" + status + "]";
        }
        return display + " [" + status + " | " + detail + "]";
    }
}
