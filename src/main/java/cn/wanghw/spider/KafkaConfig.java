package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConfig.class);


    public String getName() { return "KafkaConfig"; }
    public String getCategory() { return "mq"; }
    public String getDescription() { return "Extract Apache Kafka producer/consumer configuration"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // KafkaProducer properties
            Object clazz1 = heapHolder.findClass("org.apache.kafka.clients.producer.KafkaProducer");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("org.apache.kafka.clients.producer.ProducerConfig");
            if (clazz1 != null) {
                HashMap<String, String> fieldList1 = new HashMap<String, String>() {{
                    put("bootstrapServers", "bootstrap.servers");
                    put("saslMechanism", "sasl.mechanism");
                    put("securityProtocol", "security.protocol");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[KafkaProducer] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
            // KafkaConsumer
            Object clazz2 = heapHolder.findClass("org.apache.kafka.clients.consumer.KafkaConsumer");
            if (clazz2 == null)
                clazz2 = heapHolder.findClass("org.apache.kafka.clients.consumer.ConsumerConfig");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[KafkaConsumer] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
            // Spring Kafka properties
            Object clazz3 = heapHolder.findClass("org.springframework.boot.autoconfigure.kafka.KafkaProperties");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    result.append("[KafkaProperties] ").append(
                            HashMapUtils.dumpString(heapHolder.getAllFieldValues(instance), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
