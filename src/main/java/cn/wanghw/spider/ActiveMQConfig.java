package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConfig.class);


    public String getName() { return "ActiveMQConfig"; }
    public String getCategory() { return "mq"; }
    public String getDescription() { return "Extract Apache ActiveMQ connection configuration and credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // ActiveMQConnectionFactory
            Object clazz1 = heapHolder.findClass("org.apache.activemq.ActiveMQConnectionFactory");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("brokerURL", "brokerURL");
                    put("userName", "userName");
                    put("password", "password");
                    put("clientID", "clientID");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[ActiveMQConnectionFactory] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // ActiveMQProperties (Spring Boot)
            Object clazz2 = heapHolder.findClass("org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[ActiveMQProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
