package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQConfig.class);


    public String getName() { return "RocketMQConfig"; }
    public String getCategory() { return "mq"; }
    public String getDescription() { return "Extract Apache RocketMQ producer/consumer configuration and credentials"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // DefaultMQProducer
            Object clazz1 = heapHolder.findClass("org.apache.rocketmq.client.producer.DefaultMQProducer");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("namesrvAddr", "namesrvAddr");
                    put("producerGroup", "producerGroup");
                    put("instanceName", "instanceName");
                    put("accessKey", "accessKey");
                    put("secretKey", "secretKey");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[RocketMQProducer] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // DefaultMQPushConsumer
            Object clazz2 = heapHolder.findClass("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("namesrvAddr", "namesrvAddr");
                    put("consumerGroup", "consumerGroup");
                    put("accessKey", "accessKey");
                    put("secretKey", "secretKey");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[RocketMQConsumer] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // RocketMQProperties (Spring Boot)
            Object clazz3 = heapHolder.findClass("org.apache.rocketmq.spring.boot.RocketMQProperties");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[RocketMQProperties] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
