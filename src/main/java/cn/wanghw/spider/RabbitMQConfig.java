package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConfig.class);


    public String getName() { return "RabbitMQConfig"; }
    public String getCategory() { return "mq"; }
    public String getDescription() { return "Extract RabbitMQ connection credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // Spring RabbitMQ properties
            Object clazz1 = heapHolder.findClass("org.springframework.boot.autoconfigure.amqp.RabbitProperties");
            if (clazz1 != null) {
                HashMap<String, String> fieldList1 = new HashMap<String, String>() {{
                    put("host", "host");
                    put("port", "port");
                    put("username", "username");
                    put("password", "password");
                    put("virtualHost", "virtualHost");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[RabbitProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList1), false));
                }
            }
            // RabbitMQ ConnectionFactory
            Object clazz2 = heapHolder.findClass("com.rabbitmq.client.ConnectionFactory");
            if (clazz2 != null) {
                HashMap<String, String> fieldList2 = new HashMap<String, String>() {{
                    put("host", "host");
                    put("port", "port");
                    put("username", "username");
                    put("password", "password");
                    put("virtualHost", "virtualHost");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[RMQConnectionFactory] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList2), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
