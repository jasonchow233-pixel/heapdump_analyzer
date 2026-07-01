package cn.wanghw.rule;

import java.util.List;

public interface Validator {
    List<String> validate(List<String> candidates);
    String getName();
}
