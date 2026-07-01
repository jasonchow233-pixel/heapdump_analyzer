package cn.wanghw.rule;

import cn.wanghw.IHeapHolder;
import cn.wanghw.Severity;
import cn.wanghw.utils.HashMapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClassRule implements Rule {
    private String id;
    private String name;
    private String category;
    private Severity severity;
    private String description;
    private String className;
    private List<String> fieldNames;
    private List<String> alternateClassNames;
    private Validator validator;

    public ClassRule() {
        this.fieldNames = new ArrayList<>();
        this.alternateClassNames = new ArrayList<>();
    }

    @Override
    public RuleResult execute(IHeapHolder heapHolder) {
        List<String> results = new ArrayList<>();
        Object clazz = heapHolder.findClass(className);
        // Try alternate class names
        if (clazz == null && alternateClassNames != null) {
            for (String alt : alternateClassNames) {
                clazz = heapHolder.findClass(alt);
                if (clazz != null) break;
            }
        }
        if (clazz == null) return RuleResult.empty(this);

        for (Object instance : heapHolder.getInstances(clazz)) {
            if (fieldNames != null && !fieldNames.isEmpty()) {
                HashMap<String, String> fieldList = new HashMap<>();
                for (String field : fieldNames) {
                    fieldList.put(field, field);
                }
                HashMap<String, String> vals = heapHolder.getFieldsByNameList(instance, fieldList);
                results.add(HashMapUtils.dumpString(vals, false));
            } else {
                HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                results.add(HashMapUtils.dumpString(vals, false));
            }
        }
        return new RuleResult(this, results);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public List<String> getFieldNames() { return fieldNames; }
    public void setFieldNames(List<String> fieldNames) { this.fieldNames = fieldNames; }
    public List<String> getAlternateClassNames() { return alternateClassNames; }
    public void setAlternateClassNames(List<String> alternateClassNames) { this.alternateClassNames = alternateClassNames; }
    @Override
    public Validator getValidator() { return validator; }
    public void setValidator(Validator validator) { this.validator = validator; }
}
