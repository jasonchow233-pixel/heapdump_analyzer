package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CASConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CASConfig.class);


    public String getName() { return "CASConfig"; }
    public String getCategory() { return "auth"; }
    public String getDescription() { return "Extract CAS (Central Authentication Service) configuration and service URLs"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // CasAuthenticationProvider
            Object clazz1 = heapHolder.findClass("org.springframework.security.cas.authentication.CasAuthenticationProvider");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[CasAuthProvider] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // Cas30ServiceTicketValidator
            Object clazz2 = heapHolder.findClass("org.jasig.cas.client.validation.Cas30ServiceTicketValidator");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("org.jasig.cas.client.validation.Cas20ServiceTicketValidator");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    String casServerUrlPrefix = heapHolder.getFieldStringValue(instance, "casServerUrlPrefix");
                    result.append("[CasTicketValidator] casServerUrlPrefix=").append(casServerUrlPrefix).append("\n");
                }
            }
            // CasClientConfigProperties
            Object clazz3 = heapHolder.findClass("io.github.jhipster.security.cas.CasProperties");
            if (clazz3 == null)
                clazz3 = heapHolder.findClass("org.springframework.security.cas.CasProperties");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[CasProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
