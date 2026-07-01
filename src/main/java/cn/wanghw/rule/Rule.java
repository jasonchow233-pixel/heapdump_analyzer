package cn.wanghw.rule;

import cn.wanghw.IHeapHolder;
import cn.wanghw.Severity;

public interface Rule {
    String getId();
    String getName();
    String getCategory();
    Severity getSeverity();
    String getDescription();
    RuleResult execute(IHeapHolder heapHolder);
    default Validator getValidator() { return null; }
}
