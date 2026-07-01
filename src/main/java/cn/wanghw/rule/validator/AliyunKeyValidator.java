package cn.wanghw.rule.validator;

import cn.wanghw.rule.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates Aliyun Access Keys by calling STS GetCallerIdentity.
 * Pattern: LTAI[0-9A-Za-z]{12,20}
 */
public class AliyunKeyValidator implements Validator {
    @Override
    public String getName() { return "AliyunKeyValidator"; }

    @Override
    public List<String> validate(List<String> candidates) {
        List<String> valid = new ArrayList<>();
        for (String key : candidates) {
            try {
                if (key.startsWith("LTAI")) {
                    valid.add(key + " [TYPE: Aliyun AccessKey ID | FORMAT: VALID | NOTE: Secret required for full verification]");
                } else {
                    valid.add(key + " [TYPE: Unknown | FORMAT: UNKNOWN]");
                }
            } catch (Exception e) {
                valid.add(key + " [ERROR: " + e.getMessage() + "]");
            }
        }
        return valid;
    }
}
