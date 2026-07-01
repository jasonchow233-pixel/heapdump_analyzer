package cn.wanghw.rule.validator;

import cn.wanghw.rule.CredentialCheckResult;
import cn.wanghw.rule.CredentialStatus;
import cn.wanghw.rule.LiveValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS access key validator.
 *
 * <p>{@link #validate(List)} is offline: classifies the key type by prefix
 * ({@code AKIA} long-term, {@code ASIA} temporary session, {@code AKCA} edge function)
 * and emits an account-id hint derived from the high-order chars of the key.</p>
 *
 * <p>{@link #validateLive(List)} returns {@link CredentialStatus#UNKNOWN} for every
 * candidate. A real online check requires AWS SigV4 signing of an STS
 * {@code GetCallerIdentity} call, which needs <em>both</em> the Access Key ID and the
 * Secret Access Key. Heap strings usually only yield the Access Key ID, so a live
 * verdict cannot be safely determined. Pair this with a Spider that also extracts the
 * secret if you need true live validation.</p>
 *
 * <p>Pattern source: {@code aws-access-key.yml}.</p>
 */
public class AwsKeyValidator implements LiveValidator {

    @Override
    public String getName() {
        return "AwsKeyValidator";
    }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> out = new ArrayList<>(candidates.size());
        for (String key : candidates) {
            out.add(key + " [TYPE: " + classify(key) + " | ACCOUNT_HINT: " + accountHint(key) + "]");
        }
        return out;
    }

    @Override
    public List<CredentialCheckResult> validateLive(List<String> candidates) {
        List<CredentialCheckResult> out = new ArrayList<>(candidates.size());
        for (String key : candidates) {
            out.add(new CredentialCheckResult(key, CredentialStatus.UNKNOWN,
                    "STS GetCallerIdentity requires Access Key + Secret Access Key (SigV4)"));
        }
        return out;
    }

    private String classify(String key) {
        if (key.startsWith("AKIA")) return "Long-term Access Key";
        if (key.startsWith("ASIA")) return "Temporary Session Token";
        if (key.startsWith("AKCA")) return "CloudFront Origin Access Identity";
        if (key.startsWith("ASIA")) return "STS Temporary";
        if (key.startsWith("AK")) return "Possible AWS Key";
        return "Unknown";
    }

    private String accountHint(String key) {
        if (key.length() >= 8) {
            return key.substring(4, 8) + "***";
        }
        return "n/a";
    }
}
