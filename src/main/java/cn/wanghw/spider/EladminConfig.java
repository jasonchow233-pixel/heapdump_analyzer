package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EladminConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EladminConfig.class);


    public String getName() { return "EladminConfig"; }
    public String getCategory() { return "framework"; }
    public String getDescription() { return "Extract Eladmin framework security configuration and keys"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // SecurityProperties
            Object clazz1 = heapHolder.findClass("me.zhengjie.modules.security.config.SecurityProperties");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("me.zhengjie.config.ElAdminProperties");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[EladminProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // JwtProperties (eladmin uses jjwt)
            Object clazz2 = heapHolder.findClass("me.zhengjie.modules.security.config.bean.SecurityProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[EladminSecurityProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
