package cn.wanghw.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Validator} that can additionally perform <em>online</em> ("live") checks
 * against a provider API to determine whether a credential is still active.
 *
 * <p>Implementations MUST be safe to leave unconfigured: live validation is opt-in and
 * only invoked when the user passes {@code --validate-live}. Plain {@link #validate(List)}
 * (inherited from {@link Validator}) MUST stay offline — no network — so that
 * {@code --validate} never triggers outbound calls or cloud alerts.</p>
 *
 * <p>The default {@link #validateLive(List)} returns {@link CredentialStatus#UNKNOWN} for
 * every candidate, so validators that cannot perform a real online check (e.g. AWS,
 * whose STS GetCallerIdentity requires the secret access key in addition to the access
 * key id) can simply not override it.</p>
 */
public interface LiveValidator extends Validator {

    /**
     * Online validation. Calls the provider API for each candidate and returns a
     * {@link CredentialCheckResult} per candidate tagged
     * {@link CredentialStatus#LIVE}/{@link CredentialStatus#EXPIRED}/
     * {@link CredentialStatus#UNKNOWN}.
     *
     * <p><strong>Side-effect warning:</strong> this method performs real network I/O.
     * It must only be called when the user has explicitly enabled live validation.</p>
     *
     * @param candidates raw credential strings found in the heap
     * @return one result per candidate, never {@code null}
     */
    default List<CredentialCheckResult> validateLive(List<String> candidates) {
        List<CredentialCheckResult> out = new ArrayList<>(candidates.size());
        for (String c : candidates) {
            out.add(new CredentialCheckResult(c, CredentialStatus.UNKNOWN, "live check not supported"));
        }
        return out;
    }
}
