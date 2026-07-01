package cn.wanghw.rule;

/**
 * Result of an online ("live") credential validation against a provider API.
 *
 * <ul>
 *   <li>{@link #LIVE} — the credential is currently active and was accepted by the provider.</li>
 *   <li>{@link #EXPIRED} — the credential exists but is revoked / invalid / unauthorized.</li>
 *   <li>{@link #UNKNOWN} — the check could not determine status (e.g. rate limited, network
 *       error, or the validator only supports offline format checks for this provider).</li>
 *   <li>{@link #ERROR} — the validation attempt itself failed unexpectedly.</li>
 * </ul>
 */
public enum CredentialStatus {
    LIVE("#a6e3a1"),
    EXPIRED("#f38ba8"),
    UNKNOWN("#f9e2af"),
    ERROR("#fab387");

    private final String color;

    CredentialStatus(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
